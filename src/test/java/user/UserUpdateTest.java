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

public class UserUpdateTest {

    private static final String BASE_URL = "https://stellarburgers.nomoreparties.site/api";
    private static final String DEFAULT_PASSWORD = "TestPassword123";
    private static final String DEFAULT_NAME = "MargoUser";

    private String accessToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        String userEmail = "margo" + UUID.randomUUID() + "@test.com";

        ValidatableResponse createResponse = createUser(userEmail);
        createResponse.statusCode(200);
        accessToken = extractToken(createResponse);
    }

    @After
    public void tearDown() {
        if (accessToken != null) {
            deleteUser(accessToken);
        }
    }

    @Test
    public void updateUserDataWithAuth_shouldReturnSuccess() {
        String newName = "UpdatedName";
        String newEmail = "updated" + UUID.randomUUID() + "@test.com";

        updateUser(accessToken, newEmail, newName)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail))
                .body("user.name", equalTo(newName));
    }

    @Test
    public void updateUserDataWithoutAuth_shouldReturn401() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"noauth@test.com\", \"name\":\"NoAuthUser\"}")
                .when()
                .patch("/auth/user")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Создаём пользователя {email}")
    private ValidatableResponse createUser(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + UserUpdateTest.DEFAULT_PASSWORD + "\", \"name\":\"" + UserUpdateTest.DEFAULT_NAME + "\"}")
                .when()
                .post("/auth/register")
                .then();
    }

    @Step("Обновляем пользователя с токеном")
    private ValidatableResponse updateUser(String token, String email, String name) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"name\":\"" + name + "\"}")
                .when()
                .patch("/auth/user")
                .then();
    }

    @Step("Извлекаем accessToken")
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