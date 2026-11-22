package com.student;

import java.io.IOException;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {

    private static final String PAGE_URL = "https://books.toscrape.com/catalogue/page-";
    private static final int MAX_PAGES = 5;          // 20 books per page (site has 50 pages)
    private static final int CATALOG_SIZE = 1000;
    private static final int READING_LIST_SIZE = 20;
    private static final int EXPLORE_COUNT = 100;

    // Fixed-size arrays (no ArrayList)
    private static Book[] catalog = new Book[CATALOG_SIZE];        // all loaded books
    private static Book[] currentBooks = new Book[CATALOG_SIZE];   // last explore/search result
    private static Book[] readingList = new Book[READING_LIST_SIZE];
    private static int catalogCount = 0;
    private static int currentCount = 0;
    private static int readingCount = 0;

    private static Scanner input = new Scanner(System.in);

    public static void main(String[] args) {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter your choice: ", 0, 7);

            switch (choice) {
                case 1:
                    exploreBooks();
                    break;
                case 2:
                    String title = readText("Enter a title to search for: ");
                    if (title.isEmpty()) {
                        System.out.println("The title cannot be empty.");
                    } else {
                        searchBook(title);
                    }
                    break;
                case 3:
                    String author = readText("Enter an author to search for: ");
                    if (author.isEmpty()) {
                        System.out.println("The author cannot be empty.");
                    } else {
                        searchBook(author, true);
                    }
                    break;
                case 4:
                    addBook();
                    break;
                case 5:
                    viewReadingList();
                    break;
                case 6:
                    removeBook();
                    break;
                case 7:
                    displayBookDetails();
                    break;
                case 0:
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            System.out.println();
        } while (choice != 0);
    }

    static void printMenu() {
        System.out.println("===== GUTENBERG BOOK EXPLORER =====");
        System.out.println("1. Explore Books");
        System.out.println("2. Search by Title");
        System.out.println("3. Search by Author");
        System.out.println("4. Add Book to Reading List");
        System.out.println("5. View Reading List");
        System.out.println("6. Remove Book");
        System.out.println("7. Display Book Details");
        System.out.println("0. Exit");
    }

    // ---------------------------------------------------------------
    // JSoup: retrieve and parse the HTML of books.toscrape.com
    // ---------------------------------------------------------------
    static boolean loadCatalog() {
        if (catalogCount > 0) {
            return true; // already loaded
        }
        System.out.println("Loading books from books.toscrape.com...");
        try {
            for (int page = 1; page <= MAX_PAGES; page++) {
                Document doc = Jsoup.connect(PAGE_URL + page + ".html")
                        .userAgent("Mozilla/5.0 (GutenbergBookExplorer student project)")
                        .timeout(15000)
                        .get();

                Elements items = doc.select("article.product_pod");
                for (Element item : items) {
                    if (catalogCount == CATALOG_SIZE) {
                        break;
                    }
                    Element link = item.selectFirst("h3 a");
                    if (link == null) {
                        continue;
                    }

                    // The visible link text is shortened; the full title is in the title attribute.
                    String title = link.attr("title").trim();
                    if (title.isEmpty()) {
                        title = link.text().trim();
                    }

                    Element priceElement = item.selectFirst(".price_color");
                    String price = (priceElement != null)
                            ? priceElement.text().replace("\u00C2", "").trim()
                            : "N/A";

                    String url = link.absUrl("href");
                    if (title.isEmpty() || isInArray(catalog, catalogCount, url)) {
                        continue; // skip empty titles and duplicates
                    }

                    catalog[catalogCount] = new Book(title, price, url);
                    catalogCount++;
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }

        if (catalogCount == 0) {
            System.out.println("No books were found.");
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------
    // 1. Explore Books
    // ---------------------------------------------------------------
    static void exploreBooks() {
        if (!loadCatalog()) {
            return;
        }
        int count = Math.min(EXPLORE_COUNT, catalogCount);
        for (int i = 0; i < count; i++) {
            currentBooks[i] = catalog[i];
        }
        currentCount = count;
        displayBooks(currentBooks, currentCount, "Books");
    }

    // ---------------------------------------------------------------
    // 2 and 3. Search (overloaded methods)
    // ---------------------------------------------------------------
    static void searchBook(String title) {
        searchBook(title, false);
    }

    static void searchBook(String keyword, boolean searchByAuthor) {
        if (searchByAuthor) {
            System.out.println("books.toscrape.com does not provide author information.");
            System.out.println("Use option 2 to search by title instead.");
            return;
        }
        if (!loadCatalog()) {
            return;
        }

        String key = keyword.toLowerCase();
        currentCount = 0;
        for (int i = 0; i < catalogCount; i++) {
            if (catalog[i].getTitle().toLowerCase().contains(key)) {
                currentBooks[currentCount] = catalog[i];
                currentCount++;
            }
        }

        if (currentCount == 0) {
            System.out.println("No matching books were found.");
        } else {
            displayBooks(currentBooks, currentCount, "Search Results");
        }
    }

    static boolean isInArray(Book[] books, int size, String url) {
        for (int i = 0; i < size; i++) {
            if (books[i].getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Display
    // ---------------------------------------------------------------
    private static final int TITLE_WIDTH = 50;

    // Shortens long text so every table row keeps the same width.
    static String cut(String text, int width) {
        if (text.length() <= width) {
            return text;
        }
        return text.substring(0, width - 3) + "...";
    }

    // Prints the books as a table: number, title and price.
    static void displayBooks(Book[] books, int size, String heading) {
        String border = "+" + "-".repeat(6) + "+" + "-".repeat(TITLE_WIDTH + 2)
                + "+" + "-".repeat(10) + "+";
        String rowFormat = "| %-4s | %-" + TITLE_WIDTH + "s | %-8s |";

        System.out.println();
        System.out.println("--- " + heading + " (" + size + ") ---");
        System.out.println(border);
        System.out.println(String.format(rowFormat, "No.", "Title", "Price"));
        System.out.println(border);
        for (int i = 0; i < size; i++) {
            System.out.println(String.format(rowFormat,
                    String.valueOf(i + 1),
                    cut(books[i].getTitle(), TITLE_WIDTH),
                    books[i].getPrice()));
        }
        System.out.println(border);
    }

    // Lets the user pick a book from the last explore/search result.
    static Book chooseBook(String prompt) {
        if (currentCount == 0) {
            System.out.println("No books to choose from. Use option 1 or 2 first.");
            return null;
        }
        displayBooks(currentBooks, currentCount, "Available Books");
        int number = readInt(prompt + " (0 to cancel): ", 0, currentCount);
        if (number == 0) {
            return null;
        }
        return currentBooks[number - 1];
    }

    // ---------------------------------------------------------------
    // 4. Add Book to Reading List
    // ---------------------------------------------------------------
    static void addBook() {
        if (readingCount == readingList.length) {
            System.out.println("Your reading list is full (maximum " + READING_LIST_SIZE + " books).");
            return;
        }
        Book selected = chooseBook("Enter the number of the book to add");
        if (selected == null) {
            return;
        }
        if (isInArray(readingList, readingCount, selected.getUrl())) {
            System.out.println("This book is already in your reading list.");
            return;
        }
        readingList[readingCount] = selected;
        readingCount++;
        System.out.println("Added: " + selected.getTitle());
    }

    // ---------------------------------------------------------------
    // 5. View Reading List
    // ---------------------------------------------------------------
    static void viewReadingList() {
        if (readingCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }
        displayBooks(readingList, readingCount, "Your Reading List");
    }

    // ---------------------------------------------------------------
    // 6. Remove Book
    // ---------------------------------------------------------------
    static void removeBook() {
        if (readingCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }
        displayBooks(readingList, readingCount, "Your Reading List");
        int number = readInt("Enter the number of the book to remove (0 to cancel): ", 0, readingCount);
        if (number == 0) {
            return;
        }
        int index = number - 1;
        String removedTitle = readingList[index].getTitle();

        for (int i = index; i < readingCount - 1; i++) {
            readingList[i] = readingList[i + 1];
        }
        readingCount--;
        readingList[readingCount] = null;
        System.out.println("Removed: " + removedTitle);
    }

    // ---------------------------------------------------------------
    // 7. Display Book Details
    // ---------------------------------------------------------------
    static void displayBookDetails() {
        Book selected = chooseBook("Enter the number of the book");
        if (selected == null) {
            return;
        }
        System.out.println();
        System.out.println("Title:  " + selected.getTitle());
        System.out.println("Author: Not provided by this website");
        System.out.println("Price:  " + selected.getPrice());
        System.out.println("URL:    " + selected.getUrl());
    }

    // ---------------------------------------------------------------
    // Input helpers (Scanner)
    // ---------------------------------------------------------------
    static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!input.hasNextLine()) {
                return 0;
            }
            String line = input.nextLine().trim();
            try {
                int number = Integer.parseInt(line);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException e) {
                // handled by the message below
            }
            System.out.println("Invalid input. Enter a number from " + min + " to " + max + ".");
        }
    }

    static String readText(String prompt) {
        System.out.print(prompt);
        if (!input.hasNextLine()) {
            return "";
        }
        return input.nextLine().trim();
    }
}
