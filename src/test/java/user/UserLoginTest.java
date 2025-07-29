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

public class UserLoginTest {

    private static final String BASE_URL = "https://stellarburgers.nomoreparties.site/api";
    private static final String DEFAULT_PASSWORD = "TestPassword123";
    private static final String DEFAULT_NAME = "MargoUser";

    private String userEmail;
    private String accessToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        userEmail = "margo" + UUID.randomUUID() + "@test.com";
        // создаем пользователя для тестов
        createUser(userEmail).statusCode(200);
        // логинимся и сохраняем токен, чтобы потом удалить пользователя
        accessToken = login(userEmail, DEFAULT_PASSWORD).extract().path("accessToken");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
    }

    @After
    public void tearDown() {
        if (accessToken != null) {
            deleteUser(accessToken);
        }
    }

    @Test
    public void loginWithValidCredentials_shouldReturnSuccess() {
        login(userEmail, DEFAULT_PASSWORD)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("user.email", equalTo(userEmail));
    }

    @Test
    public void loginWithInvalidCredentials_shouldReturn401() {
        login("wrongemail@test.com", "wrongpass")
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Step("Создаем пользователя {email}")
    private ValidatableResponse createUser(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + UserLoginTest.DEFAULT_PASSWORD + "\", \"name\":\"" + UserLoginTest.DEFAULT_NAME + "\"}")
                .when()
                .post("/auth/register")
                .then();
    }

    @Step("Логинимся как {email}")
    private ValidatableResponse login(String email, String password) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}")
                .when()
                .post("/auth/login")
                .then();
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