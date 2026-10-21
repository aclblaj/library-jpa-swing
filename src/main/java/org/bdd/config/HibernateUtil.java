package org.bdd.config;

import org.bdd.model.Book;
import org.bdd.model.Subscriber;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {
    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HibernateUtil() { }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    private static SessionFactory buildSessionFactory() {
        String url = System.getenv("HIBERNATE_DB_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Set HIBERNATE_DB_URL before starting the application.");
        }

        boolean sqlite = url.startsWith("jdbc:sqlite:");
        if (!sqlite && !url.startsWith("jdbc:postgresql:")) {
            throw new IllegalStateException("HIBERNATE_DB_URL must use SQLite or PostgreSQL.");
        }

        Configuration configuration = new Configuration()
                .addAnnotatedClass(Subscriber.class)
                .addAnnotatedClass(Book.class)
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.driver_class",
                        sqlite ? "org.sqlite.JDBC" : "org.postgresql.Driver")
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .setProperty("hibernate.show_sql", "false")
                .setProperty("hibernate.connection.provider_class", "hikaricp")
                .setProperty("hibernate.hikari.maximumPoolSize", "5")
                .setProperty("hibernate.hikari.minimumIdle", "1")
                .setProperty("hibernate.hikari.poolName", "LibraryPool");

        if (sqlite) {
            configuration.setProperty("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
        }

        if (!sqlite) {
            String username = System.getenv("HIBERNATE_DB_USERNAME");
            String password = System.getenv("HIBERNATE_DB_PASSWORD");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "Set HIBERNATE_DB_USERNAME and HIBERNATE_DB_PASSWORD for PostgreSQL.");
            }
            configuration.setProperty("hibernate.connection.username", username)
                    .setProperty("hibernate.connection.password", password);
        }

        SessionFactory factory = configuration.buildSessionFactory();
        Runtime.getRuntime().addShutdownHook(new Thread(factory::close, "hibernate-shutdown"));
        return factory;
    }
}
