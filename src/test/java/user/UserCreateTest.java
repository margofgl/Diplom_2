package user;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserCreateTest {

    private static final String BASE_URL = "https://stellarburgers.nomoreparties.site/api";
    private static final String DEFAULT_PASSWORD = "TestPassword123";
    private static final String DEFAULT_NAME = "MargoUser";

    private String userEmail;
    private String accessToken; // для удаления пользователя

    @Before
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        userEmail = "margo" + UUID.randomUUID() + "@test.com"; // уникальный email перед каждым тестом
    }

    @After
    public void tearDown() {
        if (accessToken != null) {
            deleteUser(accessToken);
        }
    }

    @Test
    public void createUniqueUser_shouldReturnSuccess() {
        ValidatableResponse response = createUser(userEmail);
        response.statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(userEmail))
                .body("user.name", equalTo(DEFAULT_NAME));

        // сохраняем токен для удаления пользователя
        accessToken = extractToken(response);
    }

    @Test
    public void createExistingUser_shouldReturn403() {
        // первый запрос — успешная регистрация
        ValidatableResponse firstResponse = createUser(userEmail);
        firstResponse.statusCode(200);

        // сохраняем токен, чтобы удалить после теста
        accessToken = extractToken(firstResponse);

        // второй запрос — такой пользователь уже есть
        createUser(userEmail)
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Test
    public void createUserMissingFields_shouldReturn403() {
        // пример — отсутствует email
        given()
                .contentType("application/json")
                .body("{\"password\":\"" + DEFAULT_PASSWORD + "\",\"name\":\"" + DEFAULT_NAME + "\"}")
                .when()
                .post("/auth/register")
                .then()
                .statusCode(403)
                .body("success", equalTo(false))
                .body("message", equalTo("Email, password and name are required fields"));
    }

    // ================= Вспомогательные методы =================

    @Step("Создаём пользователя с email: {email}")
    private ValidatableResponse createUser(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + UserCreateTest.DEFAULT_PASSWORD + "\", \"name\":\"" + UserCreateTest.DEFAULT_NAME + "\"}")
                .when()
                .post("/auth/register")
                .then();
    }

    @Step("Извлекаем accessToken из ответа")
    private String extractToken(ValidatableResponse response) {
        String token = response.extract().path("accessToken");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }

    @Step("Удаляем пользователя")
    private void deleteUser(String token) {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/auth/user")
                .then()
                .statusCode(202);
    }
}