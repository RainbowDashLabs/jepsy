package de.chojo.jepsy.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.chojo.jepsy.configuration.element.Database;
import de.chojo.jepsy.configuration.element.Debug;
import de.chojo.jepsy.configuration.element.General;
import de.chojo.jepsy.configuration.exception.ConfigurationException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;

public class Configuration {
    private static final Logger log = getLogger(Configuration.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setDefaultPrettyPrinter(new DefaultPrettyPrinter())
            .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
    private General general = new General();
    private Debug debug = new Debug();
    private Database database = new Database();

    private Configuration() {
    }

    public void reload() throws IOException, ConfigurationException {
        var configuration = load();
        general = configuration.general;
        database = configuration.database;
        debug = configuration.debug;
        save();
    }

    public void save() throws IOException {
        try (var writer = MAPPER.writerWithDefaultPrettyPrinter().writeValues(getConfig().toFile())) {
            writer.write(this);
        }
    }

    public static Configuration load() throws IOException, ConfigurationException {
        forceConsistency();
        var configuration = MAPPER.readValue(getConfig().toFile(), Configuration.class);
        configuration.save();
        return configuration;
    }

    private static void forceConsistency() throws IOException {
        Files.createDirectories(getConfig().getParent());
        if (!getConfig().toFile().exists()) {
            if (getConfig().toFile().createNewFile()) {
                MAPPER.writerWithDefaultPrettyPrinter().writeValues(getConfig().toFile()).write(new Configuration());
                throw new ConfigurationException("Please configure the config.");
            }
        }
    }

    private static Path getConfig() {
        var home = new File(".").getAbsoluteFile().getParentFile().toPath();
        var property = System.getProperty("bot.config");
        if (property == null) {
            log.error("bot.config property is not set.");
            throw new ConfigurationException("Property -Dbot.config=<config path> is not set.");
        }
        return Paths.get(home.toString(), property);
    }

    public General general() {
        return general;
    }

    public Database database() {
        return database;
    }

    public Debug debug() {
        return debug;
    }
}
