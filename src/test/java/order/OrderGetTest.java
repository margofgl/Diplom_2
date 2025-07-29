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

public class OrderGetTest {

    private static final String BASE_URL = "https://stellarburgers.nomoreparties.site/api";
    private static final String DEFAULT_PASSWORD = "TestPassword123";
    private static final String DEFAULT_NAME = "MargoUser";

    private String accessToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        String userEmail = "margo" + UUID.randomUUID() + "@test.com";

        createUser(userEmail).statusCode(200);
        accessToken = extractToken(login(userEmail));
        List<String> validIngredients = getIngredients();
        createOrder(accessToken, validIngredients).statusCode(200);
    }

    @After
    public void tearDown() {
        if (accessToken != null) deleteUser(accessToken);
    }

    @Test
    public void getUserOrdersWithAuth_shouldReturnOrdersList() {
        getUserOrders(accessToken)
                .statusCode(200)
                .body("success", equalTo(true))
                .body("orders", notNullValue())
                .body("orders", not(empty()))
                .body("orders[0].ingredients", not(empty()))
                .body("orders[0].number", greaterThan(0));
    }

    @Test
    public void getUserOrdersWithoutAuth_shouldReturn401() {
        given()
                .when()
                .get("/orders")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("Получаем ингредиенты")
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
                .body("{\"email\":\"" + email + "\", \"password\":\"" + OrderGetTest.DEFAULT_PASSWORD + "\", \"name\":\"" + OrderGetTest.DEFAULT_NAME + "\"}")
                .when()
                .post("/auth/register")
                .then();
    }

    @Step("Логинимся {email}")
    private ValidatableResponse login(String email) {
        return given()
                .contentType("application/json")
                .body("{\"email\":\"" + email + "\", \"password\":\"" + OrderGetTest.DEFAULT_PASSWORD + "\"}")
                .when()
                .post("/auth/login")
                .then();
    }

    @Step("Создаём заказ для проверки GET /orders")
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

    @Step("Запрашиваем заказы пользователя")
    private ValidatableResponse getUserOrders(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/orders")
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
