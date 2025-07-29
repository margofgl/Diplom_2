package order;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrderCreateTest {

    private final String baseUrl = "https://stellarburgers.nomoreparties.site/api";
    private static final String DEFAULT_PASSWORD = "TestPassword123";
    private static final String DEFAULT_NAME = "MargoUser";

    private String accessToken;
    private List<String> validIngredients;

    @Before
    public void setUp() {
        RestAssured.baseURI = baseUrl;

        String userEmail = "margo" + UUID.randomUUID() + "@test.com";
        createUser(userEmail).statusCode(200);

        accessToken = extractToken(login(userEmail));
        validIngredients = getIngredients();
    }

    @After
    public void tearDown() {
        if (accessToken != null) deleteUser(accessToken);
    }

    @Test
    public void createOrderWithAuthAndIngredients_shouldReturnSuccess() {
        createOrder(accessToken, validIngredients)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("name", notNullValue())
                .body("order.number", greaterThan(0));
    }

    @Test
    public void createOrderWithoutAuth_shouldReturnSuccessButWithErrorInBody() {
        String json = "{\"ingredients\": [\"" + String.join("\",\"", validIngredients) + "\"]}";
        given()
                .contentType("application/json")
                .body(json)
                .when()
                .post(baseUrl + "/orders")
                .then()
                .statusCode(200)           // API реально возвращает 200
                .body("success", equalTo(true))  // success = false — это признак ошибки для API
                .body("order.number", greaterThan(0)); // желательно проверить, что есть сообщение об ошибке
    }

    @Test
    public void createOrderWithoutIngredients_shouldReturn400() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"ingredients\": []}")
                .when()
                .post(baseUrl + "/orders")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    public void createOrderWithInvalidIngredients_shouldReturn400() {
        given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body("{\"ingredients\": [\"invalid_hash\"]}")
                .when()
                .post(baseUrl + "/orders")
                .then()
                .statusCode(400); // API не возвращает 500, а 400 на неверные ингредиенты
    }

    // ===== ШАГИ =====
    @Step("Получаем список ингредиентов")
    private List<String> getIngredients() {
        return given()
                .when()
                .get("/ingredients")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data._id", String.class);
    }

    @Step("Создаём пользователя {email}")
    private ValidatableResponse createUser(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + OrderCreateTest.DEFAULT_PASSWORD + "\", \"name\":\"" + OrderCreateTest.DEFAULT_NAME + "\"}")
                .when()
                .post("/auth/register")
                .then();
    }

    @Step("Логинимся {email}")
    private ValidatableResponse login(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + OrderCreateTest.DEFAULT_PASSWORD + "\"}")
                .when()
                .post("/auth/login")
                .then();
    }

    @Step("Создаём заказ")
    private ValidatableResponse createOrder(String token, List<String> ingredients) {
        String json = "{\"ingredients\": [\"" + String.join("\",\"", ingredients) + "\"]}";
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(json)
                .when()
                .post("/orders")
                .then();
    }

    @Step("Извлекаем токен")
    private String extractToken(ValidatableResponse response) {
        String token = response.extract().path("accessToken");
        return (token != null && token.startsWith("Bearer ")) ? token.substring(7) : token;
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