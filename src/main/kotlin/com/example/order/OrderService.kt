package com.example.order

import order.OrderRequest
import order.OrderStatus
import order.OrderToProcess
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val kafkaTemplate: KafkaTemplate<String, OrderToProcess>,
) {
    companion object {
        private const val NEW_ORDERS_TOPIC = "new-orders"
        private const val WIP_ORDERS_TOPIC = "wip-orders"
        private val ITEM_NAME_REGEX = Regex("^[A-Za-z]{2,10}$")
    }

    private val serviceName = this::class.simpleName

    init {
        println("$serviceName started running..")
    }

    @KafkaListener(topics = [NEW_ORDERS_TOPIC])
    fun placeOrder(record: ConsumerRecord<String, OrderRequest>) {
        val orderRequest = record.value()
        println("[$serviceName] Received message on topic $NEW_ORDERS_TOPIC - $orderRequest")

        try {
            validateOrderRequest(orderRequest)
        } catch (e: IllegalArgumentException) {
            println("[$serviceName] Order request id: ${orderRequest.id} rejected: ${e.message}")
            throw e
        }

        val orderToProcess = OrderToProcess.newBuilder()
            .setId(orderRequest.id)
            .setStatus(OrderStatus.PROCESSING)
            .build()
        kafkaTemplate.send(WIP_ORDERS_TOPIC, orderRequest.id.toString(), orderToProcess)
        println("[$serviceName] Sent message to topic $WIP_ORDERS_TOPIC - $orderToProcess")
    }

    private fun validateOrderRequest(orderRequest: OrderRequest) {
        require(orderRequest.id in 1..100) { "Order id must be between 1 and 100" }
        orderRequest.orderItems.forEachIndexed { index, item ->
            validateItem(index, item)
        }
    }

    private fun validateItem(index: Int, item: order.Item) {
        require(item.name.length in 2..10) {
            "Order item at index $index must have a name between 2 and 10 characters"
        }
        require(ITEM_NAME_REGEX.matches(item.name)) {
            "Order item at index $index must have a name matching ${ITEM_NAME_REGEX.pattern}"
        }
        require(item.price >= 1000) {
            "Order item at index $index must have a price greater than or equal to 1000"
        }
    }
}
