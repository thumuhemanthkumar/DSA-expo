import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class LifeRPG {

    // --- ANSI Color Codes for Terminal UI ---
    private static final String RESET = "\u001B[0m";
    private static final String GOLD = "\u001B[33m";
    private static final String COMMON = "\u001B[34m";    // Blue
    private static final String UNCOMMON = "\u001B[35m";  // Purple
    private static final String LEGENDARY = "\u001B[31m"; // Red/Pink
    private static final String MUTED = "\u001B[90m";     // Gray
    private static final String GREEN = "\u001B[32m";

    // --- File for Saving State ---
    private static final String SAVE_FILE = "liferpg_save.dat";

    // --- Data Structures ---
    // -> CO2 APPLIED HERE: Designing and utilizing an Abstract Data Type (Player).
    private static Player player;
    
    // -> CO2 APPLIED HERE: Implementing a linear data structure using an array-based List.
    private static List<Quest> tasks; 
    
    // -> CO4 APPLIED HERE: Leveraging Java Collections Framework (HashMap) for a hash-based data structure demanding fast O(1) updates and lookups.
    private static Map<LocalDate, Integer> activityLog; 
    
    private static Scanner scanner = new Scanner(System.in);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // -> CO6 APPLIED HERE: Skilling to develop and create a complete program. This main loop ties the UI, logic, and data structures into a fully functional application.
    public static void main(String[] args) {
        loadGame();
        boolean running = true;

        while (running) {
            displayHUD();
            displayMenu();
            System.out.print("> ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": addQuest(); break;
                case "2": completeQuest(); break;
                case "3": deleteQuest(); break;
                case "4": displayTasks(); break;
                case "5": displayActivityLog(); break;
                case "6": 
                    saveGame();
                    running = false;
                    System.out.println("Progress saved. Logging out...");
                    break;
                default: System.out.println("Invalid command.");
            }
        }
    }

    // --- Core Logic Methods ---

    private static void addQuest() {
        System.out.println("\n--- [ DESIGN NEW QUEST ] ---");
        System.out.print("Quest Name: ");
        String name = scanner.nextLine();

        System.out.println("Select Rarity: 1. Common(50XP)  2. Uncommon(150XP)  3. Legendary(500XP)");
        System.out.print("Rarity (1-3): ");
        String rarityChoice = scanner.nextLine();
        Rarity rarity = switch (rarityChoice) {
            case "2" -> Rarity.UNCOMMON;
            case "3" -> Rarity.LEGENDARY;
            default -> Rarity.COMMON;
        };

        System.out.print("Deadline (yyyy-MM-dd HH:mm): ");
        String dateInput = scanner.nextLine();
        LocalDateTime deadline;
        try {
            deadline = LocalDateTime.parse(dateInput, formatter);
        } catch (DateTimeParseException e) {
            System.out.println(LEGENDARY + "Invalid date format. Quest creation aborted." + RESET);
            return;
        }

        // -> CO2 APPLIED HERE: Performing a typical operation (insertion) on the array-based List ADT.
        tasks.add(new Quest(name, rarity, deadline));
        System.out.println(GREEN + "Quest Initialized!" + RESET);
        saveGame();
    }

    private static void completeQuest() {
        displayTasks();
        System.out.print("\nEnter Quest ID to complete: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            Quest q = findQuestById(id);

            if (q == null || q.isDone) {
                System.out.println("Quest not found or already completed.");
                return;
            }

            q.isDone = true;
            LocalDateTime now = LocalDateTime.now();
            long diffHours = ChronoUnit.HOURS.between(now, q.deadline);

            // Calculate Multipliers
            double multiplier = 1.0;
            String msg = "Quest Complete";

            if (diffHours < 0) {
                multiplier = 0.5;
                msg = "Late Completion (Penalty Applied)";
            } else if (diffHours > 4) {
                multiplier = 1.5;
                msg = "Speed Bonus! (Completed way ahead of schedule)";
            }

            int totalXP = (int) (q.rarity.baseXp * multiplier);
            player.gainXp(totalXP);

            // Update Activity Log (HashMap)
            LocalDate today = LocalDate.now();
            
            // -> CO4 APPLIED HERE: Using fast hash-based lookups (getOrDefault) and updates (put) for scalable data handling.
            activityLog.put(today, activityLog.getOrDefault(today, 0) + 1);

            System.out.println(GOLD + msg + "! Earned +" + totalXP + " XP." + RESET);
            saveGame();

        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    private static void deleteQuest() {
        displayTasks();
        System.out.print("\nEnter Quest ID to delete: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            
            // -> CO2 APPLIED HERE: Performing a typical operation (deletion) on the linear data structure.
            boolean removed = tasks.removeIf(q -> q.id == id);
            
            if (removed) {
                System.out.println("Quest deleted from the log.");
                saveGame();
            } else {
                System.out.println("Quest not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID.");
        }
    }

    // --- UI Rendering Methods ---

    private static void displayHUD() {
        System.out.println("\n=================================================");
        System.out.printf(GOLD + " PLAYER STATUS: Level %d " + RESET + "| " + COMMON + "XP: %d / %d" + RESET + "\n", 
                          player.level, player.xp, player.nextLevelXp);
        
        // Simple Text-Based XP Bar
        int barLength = 20;
        int filled = (int) (((double) player.xp / player.nextLevelXp) * barLength);
        System.out.print(" [");
        for(int i=0; i<barLength; i++) {
            if(i < filled) System.out.print(GOLD + "=" + RESET);
            else System.out.print(MUTED + "-" + RESET);
        }
        System.out.println("]");
        System.out.println("=================================================");
    }

    private static void displayMenu() {
        System.out.println("1. Add Quest  |  2. Complete Quest  |  3. Delete Quest");
        System.out.println("4. View Quests|  5. Activity Log    |  6. Save & Exit");
    }

    private static void displayTasks() {
        System.out.println("\n--- [ ACTIVE QUESTS ] ---");
        if (tasks.isEmpty()) {
            System.out.println(MUTED + "No quests found. Time to relax... or plan!" + RESET);
            return;
        }

        // -> CO1 APPLIED HERE: Analyzing alternative solutions and implementing a custom sorting algorithm 
        // to order items by multiple input constraints (completion status, then deadline).
        tasks.sort(Comparator.comparing((Quest q) -> q.isDone).thenComparing(q -> q.deadline));

        // -> CO2 APPLIED HERE: Performing a typical operation (traversing) through the linear data structure.
        for (Quest q : tasks) {
            String color = switch (q.rarity) {
                case COMMON -> COMMON;
                case UNCOMMON -> UNCOMMON;
                case LEGENDARY -> LEGENDARY;
            };
            
            String status = q.isDone ? GREEN + "[✓]" + RESET : "[ ]";
            String deadlineStr = q.deadline.format(formatter);
            if (!q.isDone && q.deadline.isBefore(LocalDateTime.now())) {
                deadlineStr = LEGENDARY + deadlineStr + " (OVERDUE)" + RESET;
            }

            System.out.printf("%s ID: %d | %s%s%s [%s] - Due: %s\n", 
                              status, q.id, color, q.name, RESET, q.rarity, deadlineStr);
        }
    }

    private static void displayActivityLog() {
        System.out.println("\n--- [ RECENT ACTIVITY (Last 7 Days) ] ---");
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            
            // -> CO4 APPLIED HERE: Efficient retrieval from the hash table.
            int count = activityLog.getOrDefault(date, 0);
            System.out.printf("%s : %d Quests Completed\n", date, count);
        }
    }

    // --- Helper Methods & Persistence ---

    private static Quest findQuestById(int id) {
        for (Quest q : tasks) {
            if (q.id == id) return q;
        }
        return null;
    }

    // -> CO5 APPLIED HERE: Developing common practical applications for linear Data Structures 
    // by handling serialization so the data persists between real-world use sessions.
    @SuppressWarnings("unchecked")
    private static void loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
            player = (Player) ois.readObject();
            tasks = (List<Quest>) ois.readObject();
            activityLog = (Map<LocalDate, Integer>) ois.readObject();
        } catch (FileNotFoundException e) {
            // First time running
            player = new Player();
            tasks = new ArrayList<>();
            activityLog = new HashMap<>();
        } catch (Exception e) {
            System.out.println("");             
            player = new Player();
            tasks = new ArrayList<>();
            activityLog = new HashMap<>();
        }
        Quest.idCounter = tasks.stream().mapToInt(q -> q.id).max().orElse(0) + 1;
    }

    private static void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(player);
            oos.writeObject(tasks);
            oos.writeObject(activityLog);
        } catch (IOException e) {
            System.out.println("");
        }
    }

    // --- Inner Classes for DSA Elements ---

    enum Rarity {
        COMMON(50), UNCOMMON(150), LEGENDARY(500);
        final int baseXp;
        Rarity(int baseXp) { this.baseXp = baseXp; }
    }

    // -> CO2 APPLIED HERE: Designing an Abstract Data Type (ADT) to encapsulate Player state.
    static class Player implements Serializable {
        private static final long serialVersionUID = 1L;
        int level = 1;
        int xp = 0;
        int nextLevelXp = 200;

        void gainXp(int amount) {
            this.xp += amount;
            if (this.xp >= this.nextLevelXp) {
                this.level++;
                this.xp -= this.nextLevelXp;
                this.nextLevelXp = (int) (this.nextLevelXp * 1.4);
                System.out.println(GOLD + "\n*** LEVEL UP! WELCOME TO LEVEL " + this.level + " ***" + RESET);
            }
        }
    }

    // -> CO2 APPLIED HERE: Designing an Abstract Data Type (ADT) to model a Quest node.
    static class Quest implements Serializable {
        private static final long serialVersionUID = 1L;
        static transient int idCounter = 1;

        int id;
        String name;
        Rarity rarity;
        LocalDateTime deadline;
        boolean isDone;

        Quest(String name, Rarity rarity, LocalDateTime deadline) {
            this.id = idCounter++;
            this.name = name;
            this.rarity = rarity;
            this.deadline = deadline;
            this.isDone = false;
        }
    }
}