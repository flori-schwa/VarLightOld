package me.shawlaf.varlight.persistence.migrate;

import java.io.File;

public interface Migration {
    boolean migrate(File dataBaseFile);
}