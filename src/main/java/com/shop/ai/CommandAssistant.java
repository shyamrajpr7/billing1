package com.shop.ai;

import com.shop.dao.CustomerDAO;
import com.shop.dao.DiscountDAO;
import com.shop.dao.ExpenseDAO;
import com.shop.dao.ProductDAO;
import com.shop.dao.SaleDAO;
import com.shop.dao.SalesTargetDAO;
import com.shop.dao.UserDAO;
import com.shop.model.Customer;
import com.shop.model.Discount;
import com.shop.model.Expense;
import com.shop.model.Product;
import com.shop.model.Sale;
import com.shop.model.SaleItem;
import com.shop.util.SessionManager;
import com.shop.view.DashboardView;
import com.shop.view.HomeView;
import com.shop.view.POSView;
import javafx.application.Platform;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.LocalDate;

/**
 * Offline command assistant. Turns natural-language (typed or spoken) text into
 * actions: navigation, product/customer/supplier management, sales and reports.
 */
public class CommandAssistant {

    private static final double TAX_RATE = 0.05;

    private static CommandAssistant instance;

    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SalesTargetDAO targetDAO = new SalesTargetDAO();
    private final DiscountDAO discountDAO = new DiscountDAO();

    private DashboardView dashboard;
    private final Map<Integer, Integer> cart = new LinkedHashMap<>(); // productId -> qty
    private String paymentMethod = "Cash";
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "assistant-llm");
        t.setDaemon(true);
        return t;
    });

    private CommandAssistant() {
    }

    public static synchronized CommandAssistant getInstance() {
        if (instance == null) {
            instance = new CommandAssistant();
        }
        return instance;
    }

    public void setDashboard(DashboardView dashboard) {
        this.dashboard = dashboard;
    }

    /**
     * Processes the input without blocking the caller (run off the UI thread),
     * then delivers the reply on the JavaFX application thread.
     */
    public void processAsync(String rawInput, Consumer<String> onReply) {
        executor.execute(() -> {
            String reply = process(rawInput);
            Platform.runLater(() -> onReply.accept(reply));
        });
    }

    public String process(String rawInput) {
        if (rawInput == null) {
            return "I couldn't hear that. Please try again.";
        }
        String text = rawInput.trim();
        if (text.isEmpty()) {
            return "Please say or type a command.";
        }
        String lower = text.toLowerCase();

        try {
            if (matchesAny(lower, "who are you", "what can you do", "your name")) {
                return intro();
            }
            if (matchesAny(lower, "help", "what commands", "how do i", "show commands")) {
                return help();
            }

            String reply = handleNavigation(lower);
            if (reply != null) return reply;

            reply = handleReports(lower);
            if (reply != null) return reply;

            reply = handleSettings(lower, text);
            if (reply != null) return reply;

            reply = handleProducts(lower, text);
            if (reply != null) return reply;

            reply = handleCustomers(lower, text);
            if (reply != null) return reply;

            reply = handleExpenses(lower, text);
            if (reply != null) return reply;

            reply = handleCart(lower, text);
            if (reply != null) return reply;
        } catch (Exception e) {
            return "Sorry, something went wrong while doing that: " + e.getMessage();
        }

        return fallback(text, lower);
    }

    /**
     * Handles non-command input: greetings, thanks, then the LLM.
     */
    private String fallback(String text, String lower) {
        String rest = stripGreeting(lower);
        if (rest != null) {
            if (!rest.isEmpty()) {
                String reply = tryCommands(text);
                if (reply != null) return reply;
                return askLLM(text);
            }
            return "Hello! I'm your shop assistant. Try \"open inventory\", \"add product milk at 40\", or \"help\".";
        }
        if (matchesAny(lower, "thank you", "thanks", "thank")) {
            return "You're welcome! Let me know if you need anything else.";
        }
        return askLLM(text);
    }

    /**
     * If the input begins with a greeting, returns whatever follows (trimmed of
     * punctuation), or {@code ""} for a pure greeting, or {@code null} if the
     * input is not a greeting.
     */
    private String stripGreeting(String lower) {
        String[] prefixes = {"hello", "good morning", "good afternoon", "good evening", "good day",
                "how are you", "howdy", "hey", "hai", "hii", "hiii", "yo", "namaste", "hola", "bonjour", "hi"};
        String t = lower.trim();
        for (String p : prefixes) {
            if (t.startsWith(p)) {
                String rest = t.substring(p.length()).trim();
                while (!rest.isEmpty() && ".,!?:; ".indexOf(rest.charAt(0)) >= 0) {
                    rest = rest.substring(1).trim();
                }
                return rest;
            }
        }
        return null;
    }

    /**
     * No deterministic command matched: let the LLM answer. If the LLM wraps a
     * command in {@code <<...>>}, we run that command through the deterministic
     * handlers so real actions still work.
     */
    private String askLLM(String text) {
        LLMAssistant llm = LLMAssistant.getInstance();
        if (!llm.isConfigured()) {
            return "I didn't understand that. Say or type \"help\" to see what I can do.";
        }
        try {
            String reply = llm.chatWithWeb(systemPrompt(), text);
            Matcher m = Pattern.compile("<<([^>]+)>>").matcher(reply);
            if (m.find()) {
                String command = m.group(1).trim();
                String executed = tryCommands(command);
                if (executed != null) return executed;
            }
            return reply;
        } catch (Exception e) {
            return "I couldn't reach my AI brain right now. Say or type \"help\" to use built-in commands. (" + e.getMessage() + ")";
        }
    }

    private String tryCommands(String text) {
        String lower = text.toLowerCase();
        String reply;
        reply = handleNavigation(lower);
        if (reply != null) return reply;
        reply = handleReports(lower);
        if (reply != null) return reply;
        reply = handleSettings(lower, text);
        if (reply != null) return reply;
        reply = handleProducts(lower, text);
        if (reply != null) return reply;
        reply = handleCustomers(lower, text);
        if (reply != null) return reply;
        reply = handleExpenses(lower, text);
        if (reply != null) return reply;
        reply = handleCart(lower, text);
        if (reply != null) return reply;
        return null;
    }

    private String systemPrompt() {
        return "You are Grok, the friendly AI assistant built into a shop management desktop app. "
                + "You can also act on the shop. These actions are supported, using this exact syntax:\n"
                + "- Navigation: \"open dashboard\", \"open pos\", \"open inventory\", \"open customers\", "
                + "\"open discounts\", \"open suppliers\", \"open reports\", \"open employees\"\n"
                + "- Products: \"add product <name> at <sell price>\" (optional \"buy <price>\", \"qty <n>\"), "
                + "\"delete product <name>\", \"check stock of <name>\", \"low stock\", \"expiring products\"\n"
                + "- Customers: \"add customer <name>\" (optional \"phone <n>\", \"email <e>\"), "
                + "\"search customer <name>\", \"how many customers\"\n"
                + "- Expenses: \"add expense <description> <amount>\", \"today's expenses\", "
                + "\"this month's expenses\", \"today's profit\", \"this month's profit\"\n"
                + "- Targets: \"set monthly target to <amount>\" (optional \"for january\" or \"next month\"), "
                + "\"what is the monthly target\"\n"
                + "- Prices & stock: \"set price of <name> to <amount>\", "
                + "\"set minimum stock of <name> to <n>\"\n"
                + "- Discounts: \"add discount <code> <value> percent\" or \"... flat\"\n"
                + "- Sales: \"add <name> to cart\", \"add <qty> <name> to cart\", \"remove <name> from cart\", "
                + "\"show cart\", \"clear cart\", \"pay by cash/card/upi/net banking\", \"checkout\"\n"
                + "- Reports: \"today's revenue\", \"revenue this month\", \"sales today\", \"total sales\", "
                + "\"total products\", \"how many customers\", \"low stock\", \"check stock of <name>\", \"expiring products\"\n"
                + "Shop data questions map to these commands (use your best judgement for phrasing):\n"
                + "- \"today's revenue / today's earnings / how much did we make today / how much money today\" → <<today's revenue>>\n"
                + "- \"this month's revenue / monthly earnings\" → <<revenue this month>>\n"
                + "- \"how many sales today / today's transactions / what's today's transaction\" → <<sales today>>\n"
                + "- \"total sales / how many transactions in total\" → <<total sales>>\n"
                + "- \"how many products / how many items\" → <<total products>>\n"
                + "- \"how many customers\" → <<how many customers>>\n"
                + "- \"which items are low on stock\" → <<low stock>>\n"
                + "- \"which products are expiring / nearing expiry\" → <<expiring products>>\n"
                + "- \"today's expenses / how much did we spend today\" → <<today's expenses>>\n"
                + "- \"today's profit / are we making money today\" → <<today's profit>>\n"
                + "- \"record an expense\" → <<add expense <description> <amount>>>\n"
                + "- \"set the monthly target to <amount> / set sales target for <month> to <amount>\" → <<set monthly target to 50000>>\n"
                + "- \"what's the monthly target / show our target\" → <<what is the monthly target>>\n"
                + "- \"change the price of <name> / set new price of <name> to <amount>\" → <<set price of <name> to 45>>\n"
                + "- \"set minimum stock / low stock alert for <name> to <n>\" → <<set minimum stock of <name> to 10>>\n"
                + "- \"add a discount coupon <code> <value> percent\" → <<add discount SAVE10 10 percent>>\n"
                + "Rules:\n"
                + "1. If the user asks about the shop's own data (sales, revenue, transactions, products, stock, "
                + "customers) in ANY wording, reply with ONLY the matching command wrapped in double angle brackets, "
                + "e.g. <<today's revenue>> or <<check stock of milk>>, and nothing else. Never answer shop-data "
                + "questions from memory or web search — the command pulls the real number from the database.\n"
                + "2. If the user asks for an action (add product, checkout, open a screen) reply with ONLY the "
                + "exact command in double angle brackets.\n"
                + "3. Otherwise (greetings, small talk, world knowledge, general questions) reply naturally, briefly "
                + "and helpfully. You have live web search: for factual or current questions, search the web and give "
                + "an accurate, precise answer with sources.\n"
                + "4. Never invent commands outside the list above. If something can't be done, say so in plain text.";
    }

    // ----------------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------------
    private String handleNavigation(String lower) {
        if (matchesAny(lower, "open dashboard", "go to dashboard", "show dashboard", "take me to dashboard", "home")) {
            navigate("Dashboard", () -> new HomeView(dashboard).getView());
            return "Opening the Dashboard.";
        }
        if (matchesAny(lower, "open pos", "point of sale", "go to pos", "open the pos", "new sale")) {
            navigate("Point of Sale", () -> new POSView().getView());
            return "Opening the Point of Sale screen.";
        }
        if (matchesAny(lower, "open inventory", "open stock", "open products", "go to inventory", "go to products")) {
            navigate("Inventory Management", () -> new com.shop.view.InventoryView().getView());
            return "Opening Inventory.";
        }
        if (matchesAny(lower, "open reorder", "reorder suggestions", "restock suggestions", "what should i reorder",
                "go to reorder", "open restock")) {
            navigate("Reorder Suggestions", () -> new com.shop.view.ReorderSuggestionsView().getView());
            return "Opening Smart Reorder Suggestions.";
        }
        if (matchesAny(lower, "open customers", "go to customers", "show customers")) {
            navigate("Customer Management", () -> new com.shop.view.CustomerView().getView());
            return "Opening Customer Management.";
        }
        if (matchesAny(lower, "open discounts", "open coupons", "go to discounts", "discount management")) {
            navigate("Discount Management", () -> new com.shop.view.DiscountView().getView());
            return "Opening Discounts & Coupons.";
        }
        if (matchesAny(lower, "open suppliers", "go to suppliers", "show suppliers")) {
            navigate("Supplier Management", () -> new com.shop.view.SupplierView().getView());
            return "Opening Supplier Management.";
        }
        if (matchesAny(lower, "open reports", "open sales report", "go to reports", "open analytics", "go to sales and analytics")) {
            navigate("Sales & Reports", () -> new com.shop.view.ReportsView().getView());
            return "Opening Sales & Reports.";
        }
        if (matchesAny(lower, "open expenses", "go to expenses", "expense management", "open expense", "show expenses")) {
            navigate("Expense Management", () -> new com.shop.view.ExpensesView().getView());
            return "Opening Expenses & Profit.";
        }
        if (matchesAny(lower, "open employees", "go to employees", "open employee directory", "go to employee directory")) {
            navigate("Employee Management", () -> new com.shop.view.EmployeeView().getView());
            return "Opening Employee Directory.";
        }
        return null;
    }

    private void navigate(String title, Supplier<Node> viewSupplier) {
        if (dashboard != null) {
            Platform.runLater(() -> dashboard.loadView(title, viewSupplier));
        }
    }

    // ----------------------------------------------------------------
    // Reports & statistics
    // ----------------------------------------------------------------
    private String handleReports(String lower) {
        if (matchesAny(lower, "today's revenue", "today revenue", "revenue today", "how much did we make today",
                "how much we made today", "how much did we earn today", "earnings today", "today earning",
                "today's earning", "today's transaction amount", "transaction amount today",
                "sales amount today", "today's sales amount", "total for today", "how much money today")) {
            return "Today's revenue is ₹" + String.format("%.2f", saleDAO.getTotalRevenueToday()) + ".";
        }
        if (matchesAny(lower, "this month's revenue", "this month revenue", "monthly revenue", "revenue this month",
                "how much did we make this month", "earnings this month", "this month earning",
                "monthly earning", "monthly sales", "how much did we earn this month")) {
            return "This month's revenue is ₹" + String.format("%.2f", saleDAO.getTotalRevenueThisMonth()) + ".";
        }
        if (matchesAny(lower, "sales today", "transactions today", "how many sales today",
                "how many sales did we make today", "today's sales", "sales for today",
                "today's transaction", "today's transactions", "today transaction", "transaction today",
                "how many transactions today", "what are today's transactions", "what's today's transaction")) {
            return "You have " + saleDAO.getSalesCountToday() + " sale(s) today.";
        }
        if (matchesAny(lower, "total sales", "how many sales", "how many transactions", "total transactions",
                "all sales", "how many sales in total", "how many transactions in total")) {
            return "There have been " + saleDAO.findAll().size() + " sale(s) in total.";
        }
        if (matchesAny(lower, "total products", "how many products", "product count",
                "how many products do we have", "how many items", "how many items in stock")) {
            return "You have " + productDAO.count() + " product(s) in inventory.";
        }
        if (matchesAny(lower, "how many customers", "customer count", "total customers",
                "how many customers do we have", "how many customers are there")) {
            return "You have " + customerDAO.count() + " registered customer(s).";
        }
        if (matchesAny(lower, "low stock count", "how many low stock", "how many products are low")) {
            return "You have " + productDAO.countLowStock() + " product(s) at or below their minimum stock level.";
        }
        if (matchesAny(lower, "low stock", "which products are low", "stock alerts", "which items are low")) {
            List<Product> low = productDAO.findLowStock();
            if (low.isEmpty()) return "No products are low on stock. Inventory looks healthy!";
            StringBuilder sb = new StringBuilder("Low stock products (").append(low.size()).append("):");
            int shown = 0;
            for (Product p : low) {
                if (shown++ >= 10) break;
                sb.append("\n  - ").append(p.getName()).append(": ").append(p.getQuantity()).append(" left");
            }
            return sb.toString();
        }
        if (matchesAny(lower, "expiring", "expiry", "expired", "expiration", "near expiry",
                "expiring soon", "products expiring", "which products expire", "expiry alerts")) {
            List<Product> expired = productDAO.findExpired();
            List<Product> expiring = productDAO.findExpiring(30);
            if (expired.isEmpty() && expiring.isEmpty()) {
                return "No products are expired or expiring within the next 30 days.";
            }
            StringBuilder sb = new StringBuilder();
            if (!expired.isEmpty()) {
                sb.append("Expired (").append(expired.size()).append("):");
                for (Product p : expired) {
                    sb.append("\n  - ").append(p.getName()).append(" expired ").append(p.getExpiryDateLabel());
                }
            }
            if (!expiring.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("Expiring within 30 days (").append(expiring.size()).append("):");
                for (Product p : expiring) {
                    sb.append("\n  - ").append(p.getName()).append(" expires ").append(p.getExpiryDateLabel());
                }
            }
            return sb.toString();
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Products
    // ----------------------------------------------------------------
    private String handleProducts(String lower, String text) {
        // stock check: "stock of <name>" / "check stock <name>" / "how many <name> in stock"
        Matcher stockMatcher = Pattern.compile(
                "(?:check\\s+stock|stock of|stock for|how many|in stock|do we have)\\s+(?:the\\s+)?([a-z0-9 ]+)",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (matchesAny(lower, "check stock", "stock of", "stock for", "in stock", "how many") && stockMatcher.find()) {
            String q = stockMatcher.group(1).trim();
            if (!q.isEmpty() && !matchesAny(q, "low", "today", "products", "customers", "sales")) {
                Product p = findUniqueProduct(q);
                if (p == null) return "I couldn't find a single product matching \"" + q + "\".";
                return p.getName() + " has " + p.getQuantity() + " in stock (" + p.getStockStatus() + ").";
            }
        }

        if (lower.startsWith("delete product ")) {
            String name = text.substring("delete product ".length()).trim();
            Product p = findUniqueProduct(name);
            if (p == null) return "I couldn't find a single product matching \"" + name + "\" to delete.";
            productDAO.delete(p.getId());
            refresh();
            return "Deleted product \"" + p.getName() + "\".";
        }

        if (lower.startsWith("add product ")) {
            return addProduct(text);
        }

        return null;
    }

    private String addProduct(String text) {
        Matcher m = Pattern.compile("add product (.+?)(?=\\s+(with|buy|sell|cost|price|qty|quantity|stock|category|at)\\b|$)",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (!m.find()) return "Please say the product name, like \"add product milk at 40\".";
        String name = m.group(1).trim();
        if (name.isEmpty()) return "Please tell me the product name.";
        String rest = text.substring(m.end());

        double buyPrice = extractNumber(rest, "(?:buy(?:ing)?(?:\\s+price)?|cost(?:\\s+price)?)\\s*(?:is|=|:)?\\s*(\\d+(?:\\.\\d+)?)", 0);
        double sellPrice = extractNumber(rest,
                "(?:sell(?:ing)?(?:\\s+price)?|price)\\s*(?:is|=|:)?\\s*(\\d+(?:\\.\\d+)?)", -1);
        if (sellPrice < 0) {
            sellPrice = extractNumber(rest, "at\\s*(\\d+(?:\\.\\d+)?)", -1);
        }
        if (sellPrice < 0) {
            Matcher anyNum = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(rest);
            if (anyNum.find()) sellPrice = Double.parseDouble(anyNum.group(1));
        }
        int quantity = extractInt(rest, "(?:qty|quantity|stock)\\s*(?:is|=|:)?\\s*(\\d+)", 10);
        String category = extractText(rest, "category\\s+(?:is|=|:)?\\s*([a-zA-Z0-9 ]+)", "");

        Product p = new Product();
        p.setName(name);
        p.setBuyPrice(buyPrice);
        p.setSellPrice(sellPrice);
        p.setQuantity(quantity);
        p.setMinStockLevel(5);
        p.setCategory(category);

        boolean ok = productDAO.insert(p);
        if (!ok) return "Sorry, I could not add the product. It may already exist (duplicate barcode).";
        refresh();
        return "Added product \"" + name + "\" at ₹" + String.format("%.2f", sellPrice)
                + (buyPrice > 0 ? " (cost ₹" + String.format("%.2f", buyPrice) + ")" : "")
                + " with quantity " + quantity + ". What's next?";
    }

    private Product findUniqueProduct(String query) {
        List<Product> results = productDAO.search(query.trim());
        if (results.isEmpty()) {
            List<Product> all = productDAO.findAll();
            for (Product p : all) {
                if (p.getName().equalsIgnoreCase(query.trim())) return p;
            }
            return null;
        }
        if (results.size() == 1) return results.get(0);
        for (Product p : results) {
            if (p.getName().equalsIgnoreCase(query.trim())) return p;
        }
        return results.get(0);
    }

    // ----------------------------------------------------------------
    // Customers
    // ----------------------------------------------------------------
    private String handleCustomers(String lower, String text) {
        if (lower.startsWith("add customer ")) {
            Matcher m = Pattern.compile("add customer (.+?)(?=\\s+(phone|email|at)\\b|$)", Pattern.CASE_INSENSITIVE).matcher(text);
            if (!m.find()) return "Please say the customer name, like \"add customer Rahul\".";
            String name = m.group(1).trim();
            String rest = text.substring(m.end());
            String phone = extractText(rest, "phone\\s+(\\d[\\d\\s-]*)", "");
            String email = extractText(rest, "email\\s+([\\w.@-]+)", "");

            Customer c = new Customer();
            c.setName(name);
            c.setPhone(phone.trim());
            c.setEmail(email.trim());
            boolean ok = customerDAO.insert(c);
            if (!ok) return "Sorry, I could not add the customer.";
            refresh();
            return "Added customer \"" + name + "\"" + (phone.isEmpty() ? "" : " with phone " + phone) + ".";
        }

        if (lower.startsWith("search customer ") || lower.startsWith("find customer ")) {
            String name = text.replaceFirst("(?i)(search|find) customer ", "").trim();
            List<Customer> list = customerDAO.search(name);
            if (list.isEmpty()) return "No customer found matching \"" + name + "\".";
            StringBuilder sb = new StringBuilder("Found ").append(list.size()).append(" customer(s):");
            for (int i = 0; i < Math.min(list.size(), 5); i++) {
                Customer c = list.get(i);
                sb.append("\n  - ").append(c.getName()).append(" (points: ").append(c.getLoyaltyPoints()).append(")");
            }
            return sb.toString();
        }

        if (matchesAny(lower, "loyalty points of", "points of", "how many points")) {
            String name = text.replaceFirst("(?i)(loyalty points of|points of|how many points does)", "")
                    .replaceAll("(?i)have", "").replaceAll("\\?", "").trim();
            if (!name.isEmpty()) {
                List<Customer> list = customerDAO.search(name);
                if (!list.isEmpty()) {
                    Customer c = list.get(0);
                    return c.getName() + " has " + c.getLoyaltyPoints() + " loyalty points.";
                }
            }
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Expenses & profit
    // ----------------------------------------------------------------
    private String handleExpenses(String lower, String text) {
        if (matchesAny(lower, "today's expenses", "today expenses", "expenses today", "how much did we spend today",
                "how much we spent today", "spent today", "today spending", "today's spending")) {
            return "Today's expenses are ₹" + String.format("%.2f", expenseDAO.getTotalToday()) + ".";
        }
        if (matchesAny(lower, "this month's expenses", "this month expenses", "expenses this month",
                "monthly expenses", "how much did we spend this month", "spent this month")) {
            return "This month's expenses are ₹" + String.format("%.2f", expenseDAO.getTotalThisMonth()) + ".";
        }
        if (matchesAny(lower, "today's profit", "profit today", "today profit", "how much profit today",
                "how much did we profit today", "did we make a profit", "today's earning after expenses")) {
            double profit = saleDAO.getTotalRevenueToday() - expenseDAO.getTotalToday();
            return "Today's profit is ₹" + String.format("%.2f", profit)
                    + (profit >= 0 ? " (revenue ₹" + String.format("%.2f", saleDAO.getTotalRevenueToday())
                    + " − expenses ₹" + String.format("%.2f", expenseDAO.getTotalToday()) + ")."
                    : " — that's a loss (revenue ₹" + String.format("%.2f", saleDAO.getTotalRevenueToday())
                    + " − expenses ₹" + String.format("%.2f", expenseDAO.getTotalToday()) + ").");
        }
        if (matchesAny(lower, "this month's profit", "monthly profit", "profit this month",
                "how much profit this month")) {
            double profit = saleDAO.getTotalRevenueThisMonth() - expenseDAO.getTotalThisMonth();
            return "This month's profit is ₹" + String.format("%.2f", profit)
                    + (profit >= 0 ? " (revenue ₹" + String.format("%.2f", saleDAO.getTotalRevenueThisMonth())
                    + " − expenses ₹" + String.format("%.2f", expenseDAO.getTotalThisMonth()) + ")."
                    : " — that's a loss (revenue ₹" + String.format("%.2f", saleDAO.getTotalRevenueThisMonth())
                    + " − expenses ₹" + String.format("%.2f", expenseDAO.getTotalThisMonth()) + ").");
        }
        if (lower.startsWith("add expense ") || lower.startsWith("log expense ") || lower.startsWith("record expense ")) {
            return addExpense(text);
        }
        return null;
    }

    private String addExpense(String text) {
        String body = text.replaceFirst("(?i)(add|log|record) expense ", "").trim();
        Matcher amountMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(body);
        if (!amountMatcher.find()) {
            return "Please say the amount too, like \"add expense electricity 500\".";
        }
        double amount = Double.parseDouble(amountMatcher.group(1));
        String description = body.substring(0, amountMatcher.start())
                .replaceAll("[\\s,:;-]+$", "").trim();
        String category = "General";
        Matcher catMatcher = Pattern.compile("(?:for|category)\\s+([a-zA-Z ]+)",
                Pattern.CASE_INSENSITIVE).matcher(body);
        if (catMatcher.find()) {
            category = catMatcher.group(1).trim();
            description = description.replaceFirst("(?i)" + Pattern.quote(catMatcher.group(0).trim()), "").trim();
        }
        if (description.isEmpty()) {
            description = "Expense " + category;
        }

        Expense expense = new Expense();
        expense.setDescription(description);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setUserId(SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getId() : 0);
        boolean ok = expenseDAO.insert(expense);
        if (!ok) return "Sorry, I could not record the expense.";
        refresh();
        return "Recorded expense \"" + description + "\" (₹" + String.format("%.2f", amount)
                + ") under " + category + ". Today's expenses are now ₹"
                + String.format("%.2f", expenseDAO.getTotalToday()) + ".";
    }

    // ----------------------------------------------------------------
    // Cart & sales
    // ----------------------------------------------------------------
    private String handleCart(String lower, String text) {
        if (matchesAny(lower, "show cart", "what's in my cart", "whats in my cart", "current cart", "view cart")) {
            if (cart.isEmpty()) return "Your cart is empty.";
            StringBuilder sb = new StringBuilder("Your cart:");
            double total = 0;
            for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
                Product p = productDAO.findById(e.getKey());
                if (p == null) continue;
                double line = p.getSellPrice() * e.getValue();
                total += line;
                sb.append("\n  - ").append(p.getName()).append(" x").append(e.getValue())
                        .append(" = ₹").append(String.format("%.2f", line));
            }
            sb.append("\nSubtotal: ₹").append(String.format("%.2f", total));
            return sb.toString();
        }

        if (matchesAny(lower, "clear cart", "empty cart", "remove all items")) {
            cart.clear();
            return "Cart cleared.";
        }

        if (lower.startsWith("remove ") && lower.contains("from cart")) {
            String name = text.replaceFirst("(?i)remove ", "").replaceFirst("(?i)from cart.*", "").trim();
            Product p = findUniqueProduct(name);
            if (p == null) return "I couldn't find product \"" + name + "\".";
            if (cart.remove(p.getId()) == null) return "\"" + p.getName() + "\" is not in the cart.";
            return "Removed \"" + p.getName() + "\" from the cart.";
        }

        // "add <name> to cart" or "add <qty> <name>"
        if (lower.startsWith("add ") && lower.contains("cart")) {
            String rest = text.replaceFirst("(?i)^add\\s+", "").replaceFirst("(?i)to\\s+cart.*$", "").trim();
            int qty = 1;
            Matcher qm = Pattern.compile("^(\\d+)\\s+x?\\s*", Pattern.CASE_INSENSITIVE).matcher(rest);
            if (qm.find()) {
                qty = Integer.parseInt(qm.group(1));
                rest = rest.substring(qm.end()).trim();
            }
            Product p = findUniqueProduct(rest);
            if (p == null) return "I couldn't find product \"" + rest + "\".";
            if (p.getQuantity() < qty) return "\"" + p.getName() + "\" only has " + p.getQuantity() + " in stock.";
            cart.merge(p.getId(), qty, Integer::sum);
            return "Added " + qty + " x \"" + p.getName() + "\" to the cart. Say \"show cart\" or \"checkout\".";
        }

        if (matchesAny(lower, "checkout", "complete sale", "complete the sale", "finish sale", "make the sale", "process payment")) {
            return checkout();
        }

        if (matchesAny(lower, "pay by card", "pay with card", "payment card", "card payment")) {
            paymentMethod = "Card";
            return "Payment method set to Card.";
        }
        if (matchesAny(lower, "pay by upi", "pay with upi", "upi payment")) {
            paymentMethod = "UPI";
            return "Payment method set to UPI.";
        }
        if (matchesAny(lower, "net banking", "banking")) {
            paymentMethod = "Net Banking";
            return "Payment method set to Net Banking.";
        }
        if (matchesAny(lower, "pay by cash", "pay with cash", "cash payment", "cash")) {
            paymentMethod = "Cash";
            return "Payment method set to Cash.";
        }
        return null;
    }

    private String checkout() {
        if (cart.isEmpty()) return "Your cart is empty. Say \"add <product> to cart\" first.";
        List<SaleItem> items = new ArrayList<>();
        double subtotal = 0;
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            Product p = productDAO.findById(e.getKey());
            if (p == null) continue;
            SaleItem item = new SaleItem(p.getId(), p.getName(), e.getValue(), p.getSellPrice());
            items.add(item);
            subtotal += item.getTotal();
        }
        if (items.isEmpty()) return "The cart has no valid items.";

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        Sale sale = new Sale();
        sale.setInvoiceNumber(saleDAO.generateNextInvoiceNumber());
        sale.setCustomerId(0);
        sale.setUserId(SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getId() : 1);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(0);
        sale.setTax(tax);
        sale.setTotal(total);
        sale.setPaymentMethod(paymentMethod);
        for (SaleItem item : items) sale.addItem(item);

        boolean ok = saleDAO.createSale(sale);
        if (!ok) return "Checkout failed. Not enough stock for one of the items? Try again.";
        cart.clear();
        refresh();
        return "Sale completed! Invoice " + sale.getInvoiceNumber() + " for ₹"
                + String.format("%.2f", total) + " (" + paymentMethod + "). Stock updated.";
    }

    // ----------------------------------------------------------------
    // Settings: targets, prices, stock levels, discounts
    // ----------------------------------------------------------------
    private String handleSettings(String lower, String text) {
        // --- Monthly sales target (set) ---
        boolean setTargetIntent = (lower.contains("set") || lower.contains("update") || lower.contains("change"))
                && lower.contains("target");
        if (!setTargetIntent && (lower.contains("target to") || lower.contains("target at")
                || lower.contains("target of") || lower.contains("target is") || lower.contains("target ="))) {
            setTargetIntent = true;
        }
        if (setTargetIntent) {
            return setTarget(text);
        }

        // --- Monthly sales target (get) ---
        if (matchesAny(lower, "what is the monthly target", "what's the monthly target",
                "what is our target", "what's our target", "what is my target", "what's my target",
                "show monthly target", "show the monthly target", "show my target", "current target",
                "monthly target", "sales target", "target progress", "how much is the target",
                "check the target", "what is the target", "target status")) {
            return getTarget();
        }

        // --- Product price ---
        Matcher price = Pattern.compile(
                "(?:set|update|change|adjust)\\s+price\\s+(?:of|for)\\s+(.+?)\\s+(?:to|at|is|=|:)\\s+(\\d[\\d,]*(?:\\.\\d+)?)",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if ((lower.contains("set price") || lower.contains("update price") || lower.contains("change price")
                || lower.contains("adjust price") || lower.contains("new price")) && price.find()) {
            String name = price.group(1).trim().replaceAll("(?i)^the\\s+", "");
            double amount = parseAmount(price.group(2));
            if (amount <= 0) return "The price must be greater than zero.";
            Product p = findUniqueProduct(name);
            if (p == null) return "I couldn't find a single product matching \"" + name + "\".";
            p.setSellPrice(amount);
            boolean ok = productDAO.update(p);
            if (!ok) return "Sorry, I could not update the price.";
            refresh();
            return "Done! Selling price of \"" + p.getName() + "\" is now ₹" + formatRupees(amount) + ".";
        }

        // --- Minimum stock level ---
        if ((lower.contains("set") || lower.contains("update") || lower.contains("change"))
                && (lower.contains("minimum stock") || lower.contains("min stock")
                || lower.contains("low stock alert") || lower.contains("stock level"))) {
            Matcher minStock = Pattern.compile(
                    "(?:set|update|change)\\s+(?:minimum\\s+|min\\s+|low\\s+stock\\s+alert\\s+|stock\\s+level\\s+)?"
                            + "(?:stock|level|alert|limit)?\\s*(?:of|for)?\\s*(.+?)\\s+(?:to|at|is|=|:)\\s+(\\d+)",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (minStock.find()) {
                String name = minStock.group(1).trim().replaceAll("(?i)^the\\s+", "");
                int level = Integer.parseInt(minStock.group(2));
                if (level < 0) return "Minimum stock level cannot be negative.";
                Product p = findUniqueProduct(name);
                if (p == null) return "I couldn't find a single product matching \"" + name + "\".";
                p.setMinStockLevel(level);
                boolean ok = productDAO.update(p);
                if (!ok) return "Sorry, I could not update the stock level.";
                refresh();
                return "Done! \"" + p.getName() + "\" will be flagged as low when stock drops to "
                        + level + " or below.";
            }
        }

        // --- Discounts / coupons ---
        if (lower.startsWith("add discount") || lower.startsWith("create discount")
                || lower.startsWith("set discount") || lower.startsWith("add coupon")
                || lower.startsWith("create coupon")) {
            return addDiscount(text);
        }
        return null;
    }

    private String getTarget() {
        String month = currentMonth();
        double target = targetDAO.getTargetForMonth(month);
        double actual = saleDAO.getTotalRevenueThisMonth();
        if (target <= 0) {
            return "No sales target is set for " + monthLabel(month) + " yet. Say \"set monthly target to 50000\" to create one.";
        }
        double pct = actual / target * 100;
        return "Your sales target for " + monthLabel(month) + " is ₹" + formatRupees(target)
                + " and you've earned ₹" + formatRupees(actual) + " so far ("
                + String.format("%.0f", pct) + "% of target).";
    }

    private String setTarget(String text) {
        Matcher m = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)").matcher(text);
        if (!m.find()) {
            return "Please tell me the target amount too, like \"set monthly target to 50000\".";
        }
        double amount = parseAmount(m.group(1));
        if (amount <= 0) return "The target must be greater than zero.";

        String month = currentMonth();
        if (text.toLowerCase().contains("next month")) {
            month = java.time.YearMonth.now().plusMonths(1).toString();
        } else {
            Matcher ym = Pattern.compile("(20\\d{2}-\\d{2})").matcher(text);
            if (ym.find()) {
                month = ym.group(1);
            } else {
                Matcher mn = Pattern.compile("(january|february|march|april|may|june|july|august|september|october|november|december)",
                        Pattern.CASE_INSENSITIVE).matcher(text);
                if (mn.find()) {
                    int mon = monthNumber(mn.group(1));
                    int year = LocalDate.now().getYear();
                    Matcher yr = Pattern.compile("\\b(20\\d{2})\\b").matcher(text);
                    if (yr.find()) year = Integer.parseInt(yr.group(1));
                    month = year + "-" + String.format("%02d", mon);
                }
            }
        }
        if (!month.matches("\\d{4}-\\d{2}")) {
            return "I couldn't figure out the month. Try \"set monthly target to 50000\" (current month) or \"set target for january to 50000\".";
        }
        boolean ok = targetDAO.upsert(month, amount);
        if (!ok) return "Sorry, I could not save the target.";
        refresh();
        return "Done! Monthly sales target set to ₹" + formatRupees(amount) + " for " + monthLabel(month)
                + ". Say \"monthly target\" anytime to check progress.";
    }

    private String addDiscount(String text) {
        Matcher m = Pattern.compile(
                "(?:add|create|set)\\s+(?:a\\s+)?(?:discount|coupon)\\s+(?:code\\s+)?([a-zA-Z0-9_-]+)\\s+(\\d+(?:\\.\\d+)?)\\s*(percent\\s+off|flat\\s+off|percent|%|off|flat|rupees?)?",
                Pattern.CASE_INSENSITIVE).matcher(text);
        if (!m.find()) {
            return "Please say the code and value, like \"add discount SAVE10 10 percent\".";
        }
        String code = m.group(1).toUpperCase();
        double value = Double.parseDouble(m.group(2));
        String typeWord = m.group(3);
        String type = "PERCENTAGE";
        if (typeWord != null && (typeWord.toLowerCase().startsWith("flat") || typeWord.toLowerCase().startsWith("rupee"))) {
            type = "FLAT";
        }
        if (value <= 0) return "The discount value must be greater than zero.";
        if (discountDAO.findByCode(code) != null) {
            return "A coupon with code \"" + code + "\" already exists.";
        }
        Discount d = new Discount();
        d.setCode(code);
        d.setDescription("Created via AI assistant");
        d.setType(type);
        d.setValue(value);
        d.setMinPurchase(0);
        d.setStartDate(LocalDate.now());
        d.setEndDate(LocalDate.now().plusMonths(1));
        d.setActive(true);
        boolean ok = discountDAO.insert(d);
        if (!ok) return "Sorry, I could not create the discount.";
        refresh();
        return "Created discount coupon \"" + code + "\" with " + d.getValueDisplay() + " off"
                + " (valid for 1 month). Customers can use it at checkout.";
    }

    private int monthNumber(String name) {
        String[] months = {"january", "february", "march", "april", "may", "june", "july",
                "august", "september", "october", "november", "december"};
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(name.toLowerCase())) return i + 1;
        }
        return LocalDate.now().getMonthValue();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private void refresh() {
        if (dashboard != null) Platform.runLater(dashboard::reloadCurrentView);
    }

    private boolean matchesAny(String lower, String... phrases) {
        for (String phrase : phrases) {
            if (lower.contains(phrase)) return true;
        }
        return false;
    }

    private double extractNumber(String text, String regex, double fallback) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return fallback;
    }

    private int extractInt(String text, String regex, int fallback) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return fallback;
    }

    private String extractText(String text, String regex, String fallback) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return fallback;
    }

    private double parseAmount(String s) {
        return Double.parseDouble(s.replace(",", ""));
    }

    private String formatRupees(double v) {
        return String.format("%,.0f", v);
    }

    private String currentMonth() {
        return LocalDate.now().toString().substring(0, 7);
    }

    private String monthLabel(String month) {
        try {
            return java.time.YearMonth.parse(month)
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"));
        } catch (Exception e) {
            return month;
        }
    }

    private String intro() {
        return "I'm your shop assistant. I work completely offline. I can open any screen, "
                + "add and manage products, add customers, process sales from the cart, "
                + "and report revenue and stock stats. Say or type \"help\" to see everything.";
    }

    private String help() {
        return "Here's what I can do:\n"
                + "  • Open screens: \"open inventory\", \"open customers\", \"open pos\", \"open reports\"\n"
                + "  • Products: \"add product milk at 40\", \"delete product <name>\", \"check stock of <name>\", \"low stock\", \"expiring products\"\n"
                + "  • Customers: \"add customer Rahul\", \"search customer <name>\", \"how many customers\"\n"
                + "  • Targets: \"set monthly target to 50000\", \"what is the monthly target\"\n"
                + "  • Prices & stock: \"set price of milk to 45\", \"set minimum stock of milk to 10\"\n"
                + "  • Discounts: \"add discount SAVE10 10 percent\"\n"
                + "  • Sales: \"add milk to cart\", \"add 2 coke to cart\", \"show cart\", \"checkout\"\n"
                + "  • Reports: \"today's revenue\", \"revenue this month\", \"sales today\"";
    }
}
