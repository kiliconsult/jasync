package com.kili.jasync;

import com.kili.jasync.environment.Exchange;

@Exchange("test_exchange")
public record TestMessage(String message) {

}
