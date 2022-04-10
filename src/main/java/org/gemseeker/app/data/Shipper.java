package org.gemseeker.app.data;

import org.gemseeker.app.data.frameworks.IEntry;

/**
 *
 * @author Gem
 */
public class Shipper implements IEntry {
    
    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String insertSQL() {
        return String.format("INSERT INTO shippers (name) VALUES ('%s')", name);
    }
    
    @Override
    public String toString() {
        return name;
    }

}
