package com.example.order

import order.Item
import order.OrderRequest
import order.OrderStatus
import order.OrderToProcess
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.kafka.core.KafkaTemplate

class OrderServiceTest {
    @Suppress("UNCHECKED_CAST")
    private val kafkaTemplate = mock(KafkaTemplate::class.java) as KafkaTemplate<String, OrderToProcess>
    private val service = OrderService(kafkaTemplate)

    @Test
    fun `publishes processing order for valid request`() {
        val request = OrderRequest.newBuilder()
            .setId(1)
            .setOrderItems(
                listOf(
                    Item.newBuilder()
                        .setId(10)
                        .setName("IPhone")
                        .setQuantity(1)
                        .setPrice(5000)
                        .build()
                )
            )
            .build()

        service.placeOrder(ConsumerRecord("new-orders", 0, 0L, "1", request))

        val orderCaptor = ArgumentCaptor.forClass(OrderToProcess::class.java)
        verify(kafkaTemplate).send(org.mockito.Mockito.eq("wip-orders"), org.mockito.Mockito.eq("1"), orderCaptor.capture())
        assertEquals(1, orderCaptor.value.id)
        assertEquals(OrderStatus.PROCESSING, orderCaptor.value.status)
    }

    @Test
    fun `rejects order ids outside avro range`() {
        val request = OrderRequest.newBuilder()
            .setId(101)
            .setOrderItems(listOf(validItem()))
            .build()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.placeOrder(ConsumerRecord("new-orders", 0, 0L, "101", request))
        }

        assertEquals("Order id must be between 1 and 100", exception.message)
        verify(kafkaTemplate, never()).send(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any())
    }

    @Test
    fun `rejects item names that do not match avro constraints`() {
        val request = OrderRequest.newBuilder()
            .setId(1)
            .setOrderItems(listOf(item(name = "a1")))
            .build()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.placeOrder(ConsumerRecord("new-orders", 0, 0L, "1", request))
        }

        assertEquals("Order item at index 0 must have a name matching ^[A-Za-z]{2,10}$", exception.message)
        verify(kafkaTemplate, never()).send(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any())
    }

    @Test
    fun `rejects item prices below avro minimum`() {
        val request = OrderRequest.newBuilder()
            .setId(1)
            .setOrderItems(listOf(item(price = 999)))
            .build()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.placeOrder(ConsumerRecord("new-orders", 0, 0L, "1", request))
        }

        assertEquals("Order item at index 0 must have a price greater than or equal to 1000", exception.message)
        verify(kafkaTemplate, never()).send(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString(), org.mockito.Mockito.any())
    }

    private fun validItem(): Item = item()

    private fun item(
        name: String = "IPhone",
        price: Int = 5000,
    ): Item = Item.newBuilder()
        .setId(10)
        .setName(name)
        .setQuantity(1)
        .setPrice(price)
        .build()
}
