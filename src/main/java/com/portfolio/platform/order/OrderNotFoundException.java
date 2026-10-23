package com.portfolio.platform.order;
import java.util.UUID;
public class OrderNotFoundException extends RuntimeException { public OrderNotFoundException(UUID id){super("Order not found: "+id);} }

