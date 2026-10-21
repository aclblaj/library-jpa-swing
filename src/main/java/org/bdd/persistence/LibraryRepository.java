package org.bdd.persistence;

import org.bdd.config.HibernateUtil;
import org.bdd.model.Book;
import org.bdd.model.Subscriber;

import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.Transaction;

public final class LibraryRepository {
    public List<Subscriber> findSubscribers(String search) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (search == null || search.isBlank()) {
                return session.createQuery("from Subscriber a order by a.firstName, a.lastName", Subscriber.class)
                        .getResultList();
            }
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return session.createQuery("from Subscriber a where lower(a.firstName) like :term "
                            + "or lower(a.lastName) like :term or lower(a.email) like :term "
                            + "or lower(a.nationalId) like :term order by a.firstName, a.lastName", Subscriber.class)
                    .setParameter("term", term)
                    .getResultList();
        }
    }

    public void addSubscriber(Subscriber subscriber) {
        inTransaction(session -> session.persist(subscriber));
    }

    public List<Book> findBooks(String search) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (search == null || search.isBlank()) {
                return session.createQuery("from Book c order by c.title, c.author", Book.class)
                        .getResultList();
            }
            String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            return session.createQuery("from Book c where lower(c.title) like :term "
                            + "or lower(c.author) like :term or lower(c.isbn) like :term "
                            + "order by c.title, c.author", Book.class)
                    .setParameter("term", term)
                    .getResultList();
        }
    }

    public void addBook(Book book) {
        inTransaction(session -> session.persist(book));
    }

    public List<Book> findAvailableBooks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Book c where c.status = :status order by c.title", Book.class)
                    .setParameter("status", Book.AVAILABLE)
                    .getResultList();
        }
    }

    public List<Book> findBorrowedBooks(String subscriberName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Book c where c.status = :status "
                            + "and c.borrowedTo = :subscriber order by c.title", Book.class)
                    .setParameter("status", Book.BORROWED)
                    .setParameter("subscriber", subscriberName)
                    .getResultList();
        }
    }

    public void borrowBook(int bookId, String subscriberName) {
        inTransaction(session -> {
            Book book = session.find(Book.class, bookId);
            if (book == null || !Book.AVAILABLE.equals(book.getStatus())) {
                throw new IllegalStateException("The selected book is no longer available.");
            }
            book.setStatus(Book.BORROWED);
            book.setBorrowedTo(subscriberName);
        });
    }

    public void returnBook(int bookId, String subscriberName) {
        inTransaction(session -> {
            Book book = session.find(Book.class, bookId);
            if (book == null || !Book.BORROWED.equals(book.getStatus())
                    || !subscriberName.equals(book.getBorrowedTo())) {
                throw new IllegalStateException("The selected book is not borrowed by this subscriber.");
            }
            book.setStatus(Book.AVAILABLE);
            book.setBorrowedTo(null);
        });
    }

    private void inTransaction(SessionWork work) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                work.run(session);
                transaction.commit();
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    @FunctionalInterface
    private interface SessionWork {
        void run(Session session);
    }
}
