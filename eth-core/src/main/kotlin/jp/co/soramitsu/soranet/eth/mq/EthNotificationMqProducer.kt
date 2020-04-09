/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.mq

import com.d3.chainadapter.client.RMQConfig
import com.d3.commons.util.GsonInstance
import com.d3.commons.util.createPrettySingleThreadPool
import com.d3.notifications.event.BasicEvent
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.impl.DefaultExceptionHandler
import mu.KLogging
import kotlin.system.exitProcess

class EthNotificationMqProducer(rmqConfig: RMQConfig) {

    private val subscriberExecutorService = createPrettySingleThreadPool(
        "eth-notifications", "events_queue"
    )
    private val connectionFactory = ConnectionFactory()
    private val connection: Connection
    private val channel: Channel
    private val gson = GsonInstance.get()

    init {
        connectionFactory.host = rmqConfig.host
        connectionFactory.port = rmqConfig.port
        connectionFactory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleConnectionRecoveryException(conn: Connection, exception: Throwable) {
                logger.error("RMQ connection error", exception)
                exitProcess(1)
            }

            override fun handleUnexpectedConnectionDriverException(
                conn: Connection,
                exception: Throwable
            ) {
                logger.error("RMQ connection error", exception)
                exitProcess(1)
            }
        }
        connection = connectionFactory.newConnection(subscriberExecutorService)
        channel = connection.createChannel()
        channel.basicQos(16)
        val arguments = hashMapOf(
            // enable deduplication
            Pair("x-message-deduplication", true),
            // save deduplication data on disk rather that memory
            Pair("x-cache-persistence", "disk"),
            // save deduplication data 1 day
            Pair("x-cache-ttl", 60_000 * 60 * 24)
        )
        channel.queueDeclare(EVENTS_QUEUE_NAME, true, false, false, arguments)
    }

    /**
     * Puts event into RabbitMQ for notification service processing
     * @param event - event to put
     */
    fun enqueue(event: BasicEvent) {
        val messageProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC.builder()
            .headers(
                mapOf(
                    Pair(EVENT_TYPE_HEADER, event.javaClass.canonicalName),
                    Pair(NOTIFICATION_SERVICE_ID_HEADER, NOTIFICATION_SERVICE_NAME),
                    Pair(DEDUPLICATION_HEADER, event.id + "_" + NOTIFICATION_SERVICE_NAME)
                )
            ).build()
        val json = gson.toJson(event)
        channel.basicPublish(
            NOTIFICATION_EXCHANGE_NAME,
            EVENTS_QUEUE_NAME,
            messageProperties,
            json.toByteArray()
        )
        logger.info("Event $event has been successfully published to queue $EVENTS_QUEUE_NAME.")
    }

    companion object : KLogging() {
        const val EVENTS_QUEUE_NAME = "notification_events_queue"
        const val EVENT_TYPE_HEADER = "event_type"
        const val NOTIFICATION_SERVICE_ID_HEADER = "notification_service_id"
        const val DEDUPLICATION_HEADER = "x-deduplication-header"
        const val NOTIFICATION_SERVICE_NAME = "com.d3.notifications.service.SoraNotificationService"
        const val NOTIFICATION_EXCHANGE_NAME = ""
    }
}
