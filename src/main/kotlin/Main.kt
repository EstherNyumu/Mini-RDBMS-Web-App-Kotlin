package org.example

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import org.example.db.Database
import org.example.engine.Executor
import org.example.sql.Parser
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.http.content.defaultResource
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import org.example.api.JoinResponse
import org.example.api.OrderRequest
import org.example.api.OrderResponse
import org.example.api.UpdateOrderRequest
import org.example.api.UpdateUserRequest
import org.example.api.UserRequest
import org.example.api.UserResponse
import java.util.concurrent.atomic.AtomicInteger

fun main() {
    val db = Database()
    val executor = Executor(db)

    val userIdCounter = AtomicInteger(1)
    val orderIdCounter = AtomicInteger(101)

    executor.execute(Parser.parse("CREATE TABLE users (user_id INT PRIMARY, name TEXT, password TEXT)"))
    executor.execute(Parser.parse("CREATE TABLE orders (order_id INT PRIMARY, user_id INT, status TEXT)"))

    embeddedServer(Netty, port = 8081) {
        install(CORS) {
            anyHost()
        }
        install(ContentNegotiation) {
            json()
        }

        routing {
            static("/") {
                resources("static") // your HTML/CSS/JS
                defaultResource("index.html", "static")
            }

//            post("/init") {
//                executor.execute(Parser.parse("CREATE TABLE users (id INT PRIMARY, name TEXT)"))
//                executor.execute(Parser.parse("CREATE TABLE orders (id INT PRIMARY, user_id INT)"))
//                call.respondText("DB Initialized")
//            }

            //add user
            post("/users") {
                val user = call.receive<UserRequest>()
                val newUserId = userIdCounter.getAndIncrement()
                executor.execute(Parser.parse("INSERT INTO users VALUES ($newUserId, '${user.name}', '${user.password}')"))
                call.respondText("User registered with $newUserId")
            }

            //list all users
            get("/users") {
                val rows = executor.execute(Parser.parse("SELECT * FROM users"))
                val users = rows.map {
                    UserResponse(
                        id = it["user_id"] as Int,
                        name = it["name"] as String
                    )
                }
                call.respond(users)
            }

            //updateuser's name
            put("/users/{user_id}") {
                val id = call.parameters["user_id"]
                    ?: return@put call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                val body = call.receive<UpdateUserRequest>()
                val sql = "UPDATE users SET name = ${body.name} WHERE user_id = $id"
                executor.execute(Parser.parse(sql))
                call.respondText("User updated")
            }

            //delete user
            delete("/users/{user_id}") {
                val id = call.parameters["user_id"] ?: return@delete call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                executor.execute(Parser.parse("DELETE FROM users WHERE user_id=$id"))
                executor.execute(Parser.parse("DELETE FROM orders WHERE user_id=$id"))
                call.respondText("User deleted")
            }

            //add an order
            post("/orders") {
                val order = call.receive<OrderRequest>()
                val newOrderId = orderIdCounter.getAndIncrement()
                executor.execute(Parser.parse("INSERT INTO orders VALUES ($newOrderId, ${order.userId}, ${order.status})"))
                call.respondText("Order added with $newOrderId")
            }


            //update order status
            put("/orders/{order_id}") {
                val id = call.parameters["order_id"]
                    ?: return@put call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                val body = call.receive<UpdateOrderRequest>()
                val sql = "UPDATE orders SET status = ${body.status} WHERE order_id = $id"
                executor.execute(Parser.parse(sql))
                call.respondText("Order updated")
            }

            //delete an order
            delete("/orders/{order_id}") {
                val id = call.parameters["order_id"] ?: return@delete call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                executor.execute(Parser.parse("DELETE FROM orders WHERE order_id=$id"))
                call.respondText("Order deleted")
            }

            //list all orders
            get("/orders") {
                val rows = executor.execute(Parser.parse("SELECT * FROM orders"))

                val orders = rows.map {
                    OrderResponse(
                        id = when (val v = it["order_id"]) {
                            is Int -> v
                            is String -> v.toInt()
                            else -> throw IllegalStateException("Invalid id type")
                        },
                        userId = when (val v = it["user_id"]) {
                            is Int -> v
                            is String -> v.toInt()
                            else -> throw IllegalStateException("Invalid id type")
                        },
                        status = it["status"] as String
                    )
                }
                call.respond(orders)
            }

            //relation between users and orders
            get("/join") {
                val rows = executor.execute(Parser.parse("JOIN orders users ON user_id user_id"))
                val result = rows.map {
                    JoinResponse(
                        orderId = it["order_id"] as Int,
                        userId = it["user_id"] as Int,
                        userName = it["name"] as String,
                        status = it["status"] as String
                    )
                }

                call.respond(result)
            }
        }
    }.start(wait = true)
}
