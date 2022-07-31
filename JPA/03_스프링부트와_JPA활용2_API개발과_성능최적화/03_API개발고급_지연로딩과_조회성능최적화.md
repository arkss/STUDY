# 01. 간단한 주문 조회 V1: 엔티티를 직접 노출

주문 + 배송정보 + 회원을 조회하는 API를 만들자.

지연 로딩 때문에 발생하는 성능 문제를 단계적으로 해결해보자.

``` java
package jpabook.jpashop.api;

import jpabook.jpashop.Repository.OrderRepository;
import jpabook.jpashop.Repository.OrderSearch;
import jpabook.jpashop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        return all;
    }
 }
```

* 엔티티를 직접 노출하는 것은 좋지 않다.
* order -> member, order -> address는 지연로딩이라 실제 엔티티 대신 프록시로 존재한다. Jackson 라이브러리는 기본적으로 이 프록시 객체를 json으로 어떻게 생성해야 하는지 모르기 때문에 예외가 발생한다.

* Hibernate5Module 을 스프링 빈으로 등록하면 해결



엔티티를 직접 노출할 때 양방향 연관관게가 걸린 곳은 꼭 한 곳을 @JsonIgnore 처리 해야한다. 그렇지 않으면 양쪽을 서로 호출하면서 무한 루프가 걸린다.



Hibernate5Module를 사용하더라도 지연로딩이 걸려 있는 곳은 프록시이기 때문에 null로 표시된다.

``` java
RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAll(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
        }
        return all;
    }
 }
```

위와 같이 order를 순회하며 Lazy를 강제 초기화하여 값을 불러올 수 있다.





# 02. 간단한 주문 조회 V2: 엔티티를 DTO로 변환

``` java
@GetMapping("/api/v2/simple-orders")
public List<SimpleOrderDto> ordersV2() {
    List<Order> orders = orderRepository.findAll();
    List<SimpleOrderDto> result = orders.stream()
            .map(o -> new SimpleOrderDto(o))
            .collect(toList());

    return result;
}

@Data
static class SimpleOrderDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public SimpleOrderDto(Order order) {
        orderId = order.getId();
        name = order.getMember().getName();
        orderDate = order.getOrderDate();
        orderStatus = order.getStatus();
        address = order.getDelivery().getAddress();
    }
}
```

* 엔티티를 DTO로 변환해서 사용하는 일반적인 방법이다.
* 하지만 N+1 문제가 발생한다. 지연로딩은 영속성 컨텍스트에서 조회하므로 이미 조회된 경우 쿼리를 생략하지만 최악의 경우를 고려해야 한다.



# 03. 간단한 주문 조회 V3: 엔티티를 DTO로 변환 - 페치 조인 최적화

``` java
@GetMapping("/api/v3/simple-orders")
public List<SimpleOrderDto> orderV3() {
    List<Order> orders = orderRepository.findAllWithMemberDelivery();
    List<SimpleOrderDto> result = orders.stream()
            .map(o -> new SimpleOrderDto(o))
            .collect(toList());
    
    return result;
}
```

### OrderRepository

``` java
public List<Order> findAllWithMemberDelivery() {
    return em.createQuery(
            "select o from Order o" +
                    " join fetch o.member m" +
                    " join fetch o.delivery d", Order.class)
            .getResultList();
    )
}
```

* 엔티티를 페치 조인을 사용해서 쿼리 1번에 조회



# 04. 간단한 주문 조회 V4: JPA에서 DTO로 바로 조회

``` java
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderSimpleQueryRepository orderSimpleQueryRepository;
  
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> orderV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }
}
```



### OrderSimpleQueryRepository 조회 전용 리포지토리

``` java
package jpabook.jpashop.repository.order.simplequery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {
    private final EntityManager em;
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
```



### OrderSimpleQueryDto 리포지토리에서 DTO 직접 조회

``` java
package jpabook.jpashop.repository.order.simplequery;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.OrderStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate; //주문시간
    private OrderStatus orderStatus;
    private Address address;
    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime
            orderDate, OrderStatus orderStatus, Address address) {
        this.orderId = orderId;
        this.name = name;
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address;
    }
}
```

* 일반적인 SQL을 사용할 때 처럼 원하는 값을 선택해서 조회
* new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 변환
* SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트웍 용량 최적화
* 리포지토리에 API 스펙에 맞춘 코드가 들어가다보니 재사용성이 떨어짐



### 정리

엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두 가지 방법은 장단점이 있다. 둘 중 상황에 따라서 더 나은 방법을 선택하면 된다. 엔티티로 조회하면 리포지토리 재사용성도 좋고 개발도 단순해진다.

따라서 아래와 같은 순서를 권장한다.

1. 우선 엔티티를 DTO로 변환하는 방법을 선택한다.
2. 필요하면 페치 조인으로 성능을 최적화 한다. -> 대부분의 성능 이슈가 해결
3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접 사용한다.