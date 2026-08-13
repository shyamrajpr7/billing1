package com.shop.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Brand {
    private int id;
    private String name;
    private String manufacturer;
    private String originCountry;
    private String description;
    private String contact;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedAtLabel() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "—";
    }

    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
