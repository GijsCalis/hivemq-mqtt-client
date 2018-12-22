/*
 * Copyright 2018 The MQTT Bee project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.mqttbee.mqtt.handler.publish.incoming;

import io.netty.channel.EventLoop;
import io.reactivex.Flowable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import org.jetbrains.annotations.NotNull;
import org.mqttbee.api.mqtt.exceptions.NotConnectedException;
import org.mqttbee.api.mqtt.mqtt5.message.subscribe.Mqtt5SubscribeResult;
import org.mqttbee.mqtt.MqttClientData;
import org.mqttbee.mqtt.handler.subscribe.MqttSubscribeWithFlow;
import org.mqttbee.mqtt.handler.subscribe.MqttSubscriptionHandler;
import org.mqttbee.mqtt.ioc.ClientComponent;
import org.mqttbee.mqtt.message.subscribe.MqttSubscribe;
import org.reactivestreams.Subscriber;

/**
 * @author Silvio Giebl
 */
public class MqttSubscriptionFlowable extends Flowable<Mqtt5SubscribeResult> {

    private final @NotNull MqttSubscribe subscribe;
    private final @NotNull MqttClientData clientData;

    public MqttSubscriptionFlowable(final @NotNull MqttSubscribe subscribe, final @NotNull MqttClientData clientData) {
        this.subscribe = subscribe;
        this.clientData = clientData;
    }

    @Override
    protected void subscribeActual(final @NotNull Subscriber<? super Mqtt5SubscribeResult> subscriber) {
        if (clientData.getState().isConnectedOrReconnect()) {
            final ClientComponent clientComponent = clientData.getClientComponent();
            final MqttIncomingQosHandler incomingQosHandler = clientComponent.incomingQosHandler();
            final MqttSubscriptionHandler subscriptionHandler = clientComponent.subscriptionHandler();

            final EventLoop eventLoop = clientData.acquireEventLoop();

            final MqttSubscriptionFlow flow = new MqttSubscriptionFlow(subscriber, incomingQosHandler, eventLoop);
            subscriber.onSubscribe(flow);
            subscriptionHandler.subscribe(new MqttSubscribeWithFlow(subscribe, flow), eventLoop);
        } else {
            EmptySubscription.error(new NotConnectedException(), subscriber);
        }
    }
}
