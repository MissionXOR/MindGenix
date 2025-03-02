

# MindGenix - Stress Assessment and Management System


MindGenix is a web-based application designed to assess and manage stress levels across three key categories: **Physical Stress**, **Mental Stress**, and **Job Workload Stress**. The system provides personalized suggestions based on user responses and maintains a history of assessments for future reference.

---

## Features

- **Stress Assessment**:
  - Answer questions across three categories: Physical, Mental, and Job Workload.
  - Real-time calculation of stress levels and percentages.
- **Personalized Suggestions**:
  - Get tailored recommendations to manage stress based on your results.
- **User Profile**:
  - Update your profile information, including username, password, and profile picture.
- **Assessment History**:
  - View past stress assessment results and track progress over time.
- **Responsive Design**:
  - Fully responsive and mobile-friendly interface.

---

## Technologies Used

- **Frontend**:
  - HTML, CSS, JavaScript
  - [Bootstrap](https://getbootstrap.com/) for styling
  - [Thymeleaf](https://www.thymeleaf.org/) for server-side templating
- **Backend**:
  - [Spring Boot](https://spring.io/projects/spring-boot) for Java-based backend
  - Spring Security for authentication and authorization
- **Database**:
  - [MySQL](https://www.mysql.com/) or [H2 Database](https://www.h2database.com/) for data storage
- **Tools**:
  - [Maven](https://maven.apache.org/) for dependency management
  - [Git](https://git-scm.com/) for version control

---

## Installation

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- MySQL or H2 Database
- Maven
- Git

### Steps

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/mindgenix.git
   cd mindgenix
   ```

2. **Configure the Database**:
   - Update the `application.properties` file with your database credentials:
     ```properties
     spring.datasource.url=jdbc:mysql://localhost:3306/mindgenix
     spring.datasource.username=root
     spring.datasource.password=yourpassword
     spring.jpa.hibernate.ddl-auto=update
     ```

3. **Build the Project**:
   ```bash
   mvn clean install
   ```

4. **Run the Application**:
   ```bash
   mvn spring-boot:run
   ```

5. **Access the Application**:
   - Open your browser and navigate to `http://localhost:8080`.

---

## Usage

1. **Register/Login**:
   - Create a new account or log in with existing credentials.

2. **Take the Assessment**:
   - Answer questions in the three categories: Physical, Mental, and Job Workload.

3. **View Results**:
   - Get your stress levels and personalized suggestions.

4. **Update Profile**:
   - Change your username, password, or profile picture.

5. **View History**:
   - Check past assessment results and track your progress.

---



## Contributing

We welcome contributions! If you'd like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeatureName`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/YourFeatureName`).
5. Open a pull request.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- [Bootstrap](https://getbootstrap.com/) for the responsive design framework.
- [Spring Boot](https://spring.io/projects/spring-boot) for the backend framework.
- [Font Awesome](https://fontawesome.com/) for icons.

---

## Contact

For any questions or feedback, please reach out:

- **Email**: jovaniftaaminul1446@gmail.com
- **GitHub**: [MissionXOR](https://github.com/MissionXOR)
- **LinkedIn**: [Md Aminul Islam]([https://linkedin.com/in/your-profile](https://www.linkedin.com/in/aminul-jovan/))

