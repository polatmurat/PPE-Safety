package com.ppesafety.api.seeder;

import com.ppesafety.api.user.entity.Role;
import com.ppesafety.api.user.entity.User;
import com.ppesafety.api.user.repository.UserRepository;
import com.ppesafety.api.violation.entity.Violation;
import com.ppesafety.api.violation.repository.ViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {

        private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

        private static final List<String> LABELS = Arrays.asList("Helmet", "Vest");
        private static final List<String> LOCATIONS = Arrays.asList(
                        "Production Floor A",
                        "Warehouse B",
                        "Loading Dock",
                        "Assembly Line 1",
                        "Chemical Storage",
                        "Maintenance Area",
                        "Construction Site",
                        "Outdoor Work Area");
        private static final List<String> IMAGE_URLS = Arrays.asList(
                        "/uploads/default-violations/1.jpg",
                        "/uploads/default-violations/2.jpg",
                        "/uploads/default-violations/3.jpg",
                        "/uploads/default-violations/4.jpg",
                        "/uploads/default-violations/5.jpg",
                        "/uploads/default-violations/6.jpg",
                        "/uploads/default-violations/7.jpg");

        private final UserRepository userRepository;
        private final ViolationRepository violationRepository;
        private final PasswordEncoder passwordEncoder;

        public DataSeeder(UserRepository userRepository,
                        ViolationRepository violationRepository,
                        PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.violationRepository = violationRepository;
                this.passwordEncoder = passwordEncoder;
        }

        @Override
        @Transactional
        public void run(String... args) {
                if (userRepository.count() > 0) {
                        logger.info("Database already seeded. Skipping data initialization.");
                        return;
                }

                logger.info("Seeding database with initial data...");
                seedUsers();
                seedViolations();
                logger.info("Database seeding completed successfully!");
        }

        private void seedUsers() {
                // Create Admin
                User admin = User.builder()
                                .username("admin")
                                .password(passwordEncoder.encode("admin123"))
                                .email("admin@ppesafety.com")
                                .fullName("System Administrator")
                                .role(Role.ROLE_ADMIN)
                                .build();
                userRepository.save(admin);
                logger.info("Created admin user: {}", admin.getUsername());

                // Create Safety Specialist
                User specialist = User.builder()
                                .username("specialist")
                                .password(passwordEncoder.encode("spec123"))
                                .email("specialist@ppesafety.com")
                                .fullName("Ahmet Yılmaz (ISG Uzmanı)")
                                .role(Role.ROLE_SAFETY_SPECIALIST)
                                .build();
                userRepository.save(specialist);
                logger.info("Created safety specialist user: {}", specialist.getUsername());

                // Create 5 Employees
                String[] employeeNames = {
                                "Mehmet Demir",
                                "Ayşe Kaya",
                                "Mustafa Öztürk",
                                "Fatma Çelik",
                                "Ali Yıldız"
                };

                for (int i = 0; i < 5; i++) {
                        User employee = User.builder()
                                        .username("employee" + (i + 1))
                                        .password(passwordEncoder.encode("emp123"))
                                        .email("employee" + (i + 1) + "@ppesafety.com")
                                        .fullName(employeeNames[i])
                                        .role(Role.ROLE_EMPLOYEE)
                                        .build();
                        userRepository.save(employee);
                        logger.info("Created employee user: {}", employee.getUsername());
                }
        }

        private void seedViolations() {
                Random random = new Random(42); // Fixed seed for reproducibility

                User specialist = userRepository.findByUsername("specialist")
                                .orElseThrow(() -> new RuntimeException("Specialist not found"));

                List<User> employees = userRepository.findByRole(Role.ROLE_EMPLOYEE);

                if (employees.isEmpty()) {
                        logger.warn("No employees found. Skipping violation seeding.");
                        return;
                }

                LocalDateTime now = LocalDateTime.now();

                for (int i = 0; i < 20; i++) {
                        // Randomly select 1 to LABELS.size() labels
                        int numLabels = random.nextInt(LABELS.size()) + 1;
                        List<String> shuffledLabels = new ArrayList<>(LABELS);
                        Collections.shuffle(shuffledLabels, random);
                        List<String> selectedLabels = shuffledLabels.subList(0, numLabels);

                        // Random employee
                        User employee = employees.get(random.nextInt(employees.size()));

                        // Random location
                        String location = LOCATIONS.get(random.nextInt(LOCATIONS.size()));

                        // Sequential image URL (Round-robin)
                        String imageUrl = IMAGE_URLS.get(i % IMAGE_URLS.size());

                        // Random timestamp within last 30 days
                        LocalDateTime timestamp = now.minusDays(random.nextInt(30))
                                        .minusHours(random.nextInt(24))
                                        .minusMinutes(random.nextInt(60));

                        Violation violation = Violation.builder()
                                        .imageUrl(imageUrl)
                                        .labels(new ArrayList<>(selectedLabels))
                                        .employee(employee)
                                        .reportedBy(specialist)
                                        .location(location)
                                        .timestamp(timestamp)
                                        .build();

                        violationRepository.save(violation);
                }

                logger.info("Created 20 violation records");
        }
}
