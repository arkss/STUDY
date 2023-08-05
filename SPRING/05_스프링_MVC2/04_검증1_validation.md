# 04. 검증1 - Validation

## 검증 요구사항

### 요구사항

* 타입 검증
    * 가격, 수량에 문자가 들어가면 검증 오류 처리
* 필드 검증
    * 상품명: 필수, 공백X
    * 가격: 1000원 이상, 1백만원 이하
    * 수량: 최대 9999

* 특정 필드의 범위를 넘어서는 검증
    * 가격 * 수량의 합은 10,000원 이상



### 서버의 검증과 클라이언트의 검증

* 클라이언트 검증은 조작할 수 있으므로 보안에 취약하다.
* 서버만으로 검증하면, 즉각적인 고객 사용성이 부족해진다.
* 둘을 적절히 섞어서 사용하되, 최종적으로 서버 검증은 필수
* API 방식을 사용하면 API 스펙을 잘 정의해서 검증 오류를 API 응답 결과에 잘 남겨주어야 함



## 프로젝트 설정 V1

validation-start 폴더를 validation으로 변경하여 진행



## 검증 직접 처리 - 소개

### 상품 저장 성공

![image-20230801005723588](images/04_검증1_validation/image-20230801005723588.png)



### 상품 저장 실패

![image-20230801005830115](images/04_검증1_validation/image-20230801005830115.png)

서버 검증을 실패할 경우, 고객에게 다시 상품 등록 폼을 보여주고, 어떤 값을 잘못 입력했는지 친절하게 알려주어야 한다.



## 검증 직접 처리 - 개발

``` java
@PostMapping("/add")
public String addItem(@ModelAttribute Item item, RedirectAttributes redirectAttributes, Model model) {
  // 검증 오류 결과를 보관
  Map<String, String> errors = new HashMap<>();

  // 검증 로직
  if (!StringUtils.hasText(item.getItemName())) {
    errors.put("itemName", "상품 이름은 필수입니다.");
  }

  if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
    errors.put("price", "가격은 1,000 ~ 1,000,000 까지 허용합니다.");
  }
  if (item.getQuantity() == null || item.getQuantity() >= 9999) {
    errors.put("quantity", "수량은 최대 9,999 까지 허용합니다.");
  }

  // 특정 필드가 아닌 복합 룰 검증
  if (item.getPrice() != null && item.getQuantity() != null) {
    int resultPrice = item.getPrice() * item.getQuantity();
    if (resultPrice < 10000) {
      errors.put("globalError", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice);
    }
  }

  // 검증에 실패하면 다시 입력 폼으로
  if (!errors.isEmpty()) {
    model.addAttribute("errors", errors);
    return "validation/v1/addForm";
  }

  Item savedItem = itemRepository.save(item);
  redirectAttributes.addAttribute("itemId", savedItem.getId());
  redirectAttributes.addAttribute("status", true);
  return "redirect:/validation/v1/items/{itemId}";
}
```

* @ModelAttribute를 사용하면 Model에 넘어온 값이 자연스럽게 맵핑된다. 따라서 검증에 실패하여 "validation/v1/addForm"으로 보내도 입력폼에 데이터들이 남아있다.



### 정리

* 만약 검증 오류가 발생하면 입력 폼을 다시 보여준다.
* 검증 오류들을 고객에게 친절하게 안내해서 다시 입력할 수 있게 한다. 
* 검증 오류가 발생해도 고객이 입력한 데이터가 유지된다.
* 템플릿 관련 작업은 생략하지만 errors를 받고 이를 화면에 노출해주는 작업이 필요



### 남은 문제점

* 타입 오류 처리가 안된다. Item 의 price , quantity 같은 숫자 필드는 타입이 Integer 이므로 문자 타입으로 설정하는 것이 불가능하다. 숫자 타입에 문자가 들어오면 오류가 발생한다. 그런데 이러한 오류는 스프링MVC에서 컨트롤러에 진입하기도 전에 예외가 발생하기 때문에, 컨트롤러가 호출되지도 않고, 400 예외가 발생하면서 오류 페이지를 띄워준다.
* Item의 price에 문자를 입력하는 것 처럼 타입 오류가 발생해도 고객이 입력한 문자를 화면에 남겨야 한다. 만약 컨트롤러가 호출된다고 가정해도 Item 의 price 는 Integer 이므로 문자를 보관할 수가 없다. 결국 문자는 바인딩이 불가능하므로 고객이 입력한 문자가 사라지게 되고, 고객은 본인이 어떤 내용을 입력해서 오류가 발생했는지 이해하기 어렵다.
* 결국 고객이 입력한 값도 어딘가에 별도로 관리가 되어야 한다.



## 프로젝트 준비 V2

컨트롤러와 템플릿 파일을 복사하여 아래의 파일들을 생성해준다.

* ValidationItemControllerV2
* /resources/templates/validation/v2/
    * addForm.html
    * editForm.html
    * item.html
    * items.html



이 때 파일 내부에 url도 수정해줘야한다. validation/v1 -> validation/v2



## BindingResult1

``` java
@PostMapping("/add")
public String addItemV1(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes, Model model) {
  // 검증 오류 결과를 보관
  Map<String, String> errors = new HashMap<>();

  // 검증 로직
  if (!StringUtils.hasText(item.getItemName())) {
    bindingResult.addError(new FieldError("item", "itemName", "상품 이름은 필수입니다."));
  }

  if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
    bindingResult.addError(new FieldError("item", "price", "가격은 1,000 ~ 1,000,000 까지 허용합니다."));
  }
  if (item.getQuantity() == null || item.getQuantity() >= 9999) {
    bindingResult.addError(new FieldError("item", "quantity", "수량은 최대 9,999 까지 허용합니다."));
  }

  // 특정 필드가 아닌 복합 룰 검증
  if (item.getPrice() != null && item.getQuantity() != null) {
    int resultPrice = item.getPrice() * item.getQuantity();
    if (resultPrice < 10000) {
      bindingResult.addError(new ObjectError("item", "가격 * 수량의 합은 10,000원 이상이어야 합니다. 현재 값 = " + resultPrice));
    }
  }

  // 검증에 실패하면 다시 입력 폼으로
  if (bindingResult.hasErrors()) {
    log.info("errors={}", bindingResult);
    return "validation/v2/addForm";
  }

  Item savedItem = itemRepository.save(item);
  redirectAttributes.addAttribute("itemId", savedItem.getId());
  redirectAttributes.addAttribute("status", true);
  return "redirect:/validation/v2/items/{itemId}";
}
```

* `BindingResult bindingResult` 파라미터의 위치는 `@ModelAttribute Item item` 다음에 와야 한다.



### FieldError 생성자 요약

```java
public FieldError(String objectName, String field, String defaultMessage) {}
```

필드에 오류가 있으면 FieldError 객체를 생성해서 bindingResult 에 담아두면 된다.

* objectName : @ModelAttribute 이름
* field : 오류가 발생한 필드 이름
* defaultMessage : 오류 기본 메시지



### ObjectError 생성자 요약

``` java
public ObjectError(String objectName, String defaultMessage) {}
```

특정 필드를 넘어서는 오류가 있으면 ObjectError 객체를 생성해서 bindingResult 에 담아두면 된다.

* objectName : @ModelAttribute 의 이름
* defaultMessage : 오류 기본 메시지



## BindingResult2

### BindingResult

* 스프링이 제공하는 검증 오류를 보관하는 객체이다. 검증 오류가 발생하면 여기에 보관하면 된다.
* BindingResult가 있으면 @ModelAttribute에 데이터 바인딩 시 오류가 발생해도 컨트롤러가 호출된다.
* @ModelAttribute에 바인딩 시 타입 오류가 발생하면
    * BindingResult가 없으면 400 오류가 발생하면서 컨트롤러가 호출되지 않고 오류 페이지로 이동한다.
    * BindingResult가 있으면 오류 정보(FieldError)를 BindingResult에 담아서 컨트롤러를 정상 호출한다.



### BindingResult에 검증 오류를 적용하는 3가지 방법

* @ModelAttribute의 객체에 타입 오류 등으로 바인딩이 실패하는 경우 스프링이 FieldError 생성해서 BindingResult에 넣어준다.
* 위 코드 예시처럼 addError를 통해 개발자가 직접 넣어준다.
* Validator 사용 -> 뒤에서 설명



### 주의

* BindingResult는 검증할 대상 바로 다음에 와야한다.
* BindingResult는 Model에 자동으로 포함된다.



### BindingResult와 Errors

* org.springframework.validation.Errors 
* org.springframework.validation.BindingResult

BindingResult 는 인터페이스이고, Errors 인터페이스를 상속받고 있다. 실제 넘어오는 구현체는BeanPropertyBindingResult 라는 것인데, 둘다 구현하고 있으므로 BindingResult 대신에 Errors 를 사용해도 된다. Errors 인터페이스는 단순한 오류 저장과 조회 기능을 제공한다. BindingResult 는 여기에 더해서 추가적인 기능들을 제공한다. addError() 도 BindingResult 가 제공하므로 여기서는 BindingResult 를 사용하자. 주로 관례상 BindingResult 를 많이 사용한다.



## FieldError, ObjectError

현재는 사용자 입력 오류 시 입력한 값이 화면에 남아있지 않는다. 이를 해결해보자.



### FieldError 생성자

``` java
public FieldError(String objectName, String field, String defaultMessage);
public FieldError(String objectName, String field, @Nullable Object rejectedValue, boolean bindingFailure, @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage);
```

* objectName : 오류가 발생한 객체 이름
* field : 오류 필드
* rejectedValue : 사용자가 입력한 값(거절된 값)
* bindingFailure : 타입 오류 같은 바인딩 실패인지, 검증 실패인지 구분 값
* codes : 메시지 코드
* arguments : 메시지에서 사용하는 인자
* defaultMessage : 기본 오류 메시지

ObjectError도 유사하게 두 가지 생성자를 제공한다.



### 오류 발생 시 사용자 입력 값 유지

``` java
new FieldError("item", "price", item.getPrice(), false, null, null, "가격은 1,000 ~
1,000,000 까지 허용합니다.")
```

rejectedValue에 오류 발생 시 사용자 입력 값을 저장하여 응답한다.

타입 오류로 바인딩에 실패하면 스프링이 위와 같이 FieldError를 생성하면서 사용자가 입력한 값을 넣어둔다. 다만 bindingFailure값은 true가 될 것이다.



## 오류 코드와 메시지 처리1

### errors 메시지 파일 생성

errors.properties라는 파일을 생성하여 오류 메시지들을 별도로 관리한다.

``` properties
required.item.itemName=상품 이름은 필수입니다. 
range.item.price=가격은 {0} ~ {1} 까지 허용합니다. 
max.item.quantity=수량은 최대 {0} 까지 허용합니다. 
totalPriceMin=가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}
```



application.properties

``` properties
spring.messages.basename=messages,errors
```



### FieldError에 적용

``` java
//range.item.price=가격은 {0} ~ {1} 까지 허용합니다.
new FieldError("item", "price", item.getPrice(), false, new String[] {"range.item.price"}, new Object[]{1000, 1000000};
```

* codes : required.item.itemName 를 사용해서 메시지 코드를 지정한다. 메시지 코드는 하나가 아니라 배열로 여러 값을 전달할 수 있는데, 순서대로 매칭해서 처음 매칭되는 메시지가 사용된다.
* arguments : Object[]{1000, 1000000} 를 사용해서 코드의 {0} , {1} 로 치환할 값을 전달한다.



## 오류 코드와 메시지 처리2

### FieldError , ObjectError 생성 없이 사용하는 BindingResult

BindingResult 는 검증해야 할 객체인 target 바로 다음에 온다. 따라서 BindingResult 는 이미 본인이 검증해야 할 객체인 target 을 알고 있다.

``` java
log.info("objectName={}",bindingResult.getObjectName());
log.info("target={}", bindingResult.getTarget());
```

```
objectName=item //@ModelAttribute의 name
target=Item(id=null, itemName=상품, price=100, quantity=1234)
```



BindingResult의 rejectValue()와 reject()를 사용하면 FieldError , ObjectError를 생성하지 않고 더 깔끔하게 검증 오류를 다룰 수 있다.



### rejectValue()

``` java
void rejectValue(@Nullable String field, String errorCode,@Nullable Object[] errorArgs, @Nullable String defaultMessage);
```

* field : 오류 필드명
* errorCode : 오류 코드(이 오류 코드는 메시지에 등록된 코드가 아니다. 뒤에서 설명할 messageResolver를 위한 오류 코드이다.)
* errorArgs : 오류 메시지에서 {0} 을 치환하기 위한 값
* defaultMessage : 오류 메시지를 찾을 수 없을 때 사용하는 기본 메시지



``` java
bindingResult.rejectValue("price", "range", new Object[]{1000, 1000000}, null)
```

BindingResult는 어떤 객체를 대상으로 검증하는지 target을 이미 알고 있기 때문에 위와 같이 target의 field만 받아서 사용할 수 있다.



## 오류 코드와 메시지 처리3

오류 코드를 만들 때는 자세히 만들 수 도 있고 단순히 만들 수 도 있다.

* required.item.itemName : 상품 이름은 필수 입니다.
* required : 필수 값 입니다.



required라는 오류 코드를 사용한다고 하면 해당 오류 코드와 객체명, 필드명을 조합한 세밀한 메시지를 먼저 찾고 없으면 오류 코드의 메시지를 사용한다.

``` 
# 우선순위 1
required.item.itemName : 상품 이름은 필수 입니다.

# 우선순위 2
required : 필수 값 입니다.
```



이 경우, 메시지 properties만 변경하여 메시지를 변경할 수 있다.



## 오류 코드와 메시지 처리4

### MessageCodesResolverTest

``` java
package hello.itemservice.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;

public class MessageCodesResolverTest {

  MessageCodesResolver codesResolver = new DefaultMessageCodesResolver();

  @Test
  void messageCodesResolverObject() {
    String[] messageCodes = codesResolver.resolveMessageCodes("required", "item");
    assertThat(messageCodes).containsExactly("required.item", "required");
  }

  @Test
  void messageCodesResolverField() {
    String[] messageCodes = codesResolver.resolveMessageCodes("required", "item", "itemName", String.class);

    assertThat(messageCodes).containsExactly(
        "required.item.itemName",
        "required.itemName",
        "required.java.lang.String",
        "required"
    );
  }
}
```



### DefaultMessageCodesResolver의 메시지 생성 규칙

#### 객체 오류

```
객체 오류의 경우 다음 순서로 2가지 생성 
1.: code + "." + object name 
2.: code

예) 오류 코드: required, object name: item 
1.: required.item
2.: required
```



#### 필드 오류

```
필드 오류의 경우 다음 순서로4가지 메시지 코드 생성
1.: code + "." + object name + "." + field
2.: code + "." + field
3.: code + "." + field type
4.: code

예) 오류 코드: typeMismatch, object name "user", field "age", field type: int 
1. "typeMismatch.user.age"
2. "typeMismatch.age"
3. "typeMismatch.int"
4. "typeMismatch"
```



### 동작 방식

rejectValue(), reject()는 내부에서 MessageCodesResolver를 사용하고 여기서 메시지 코드들을 생성한다.



## 오류 코드와 메시지 처리5

모든 오류 코드에 대해서 메시지를 각각 다 정의하면 개발자 입장에서 관리하기 너무 힘들다.

크게 중요하지 않은 메시지는 범용성 있는 required 같은 메시지로 끝내고 정말 중요한 메시지는 꼭 필요할 때 구체적으로 적어서 사용하는 방식이 더 효과적이다.



errors.properties

``` properties
#required.item.itemName=상품 이름은 필수입니다. 
#range.item.price=가격은 {0} ~ {1} 까지 허용합니다. 
#max.item.quantity=수량은 최대 {0} 까지 허용합니다. 
#totalPriceMin=가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}

#==ObjectError==
#Level1
totalPriceMin.item=상품의 가격 * 수량의 합은 {0}원 이상이어야 합니다. 현재 값 = {1}
#Level2 - 생략
totalPriceMin=전체 가격은 {0}원 이상이어야 합니다. 현재 값 = {1}

#==FieldError==
#Level1
required.item.itemName=상품 이름은 필수입니다. 
range.item.price=가격은 {0} ~ {1} 까지 허용합니다. 
max.item.quantity=수량은 최대 {0} 까지 허용합니다.

#Level2 - 생략

#Level3
required.java.lang.String = 필수 문자입니다. 
required.java.lang.Integer = 필수 숫자입니다. 
min.java.lang.String = {0} 이상의 문자를 입력해주세요. 
min.java.lang.Integer = {0} 이상의 숫자를 입력해주세요. 
range.java.lang.String = {0} ~ {1} 까지의 문자를 입력해주세요. 
range.java.lang.Integer = {0} ~ {1} 까지의 숫자를 입력해주세요. 
max.java.lang.String = {0} 까지의 문자를 허용합니다. 
max.java.lang.Integer = {0} 까지의 숫자를 허용합니다.

#Level4
required = 필수 값 입니다.
min= {0} 이상이어야 합니다.
range= {0} ~ {1} 범위를 허용합니다. max= {0} 까지 허용합니다.
```



### ValidationUtils

#### ValidationUtils 사용 전

``` java
if (!StringUtils.hasText(item.getItemName())) {
  bindingResult.rejectValue("itemName", "required", "기본: 상품 이름은 필수입니다."); 
}
```



#### ValidationUtils 사용 후

``` java
ValidationUtils.rejectIfEmptyOrWhitespace(bindingResult, "itemName", "required");
```



### 정리

1. rejectValue() 호출
2. MessageCodesResolver 를 사용해서 검증 오류 코드로 메시지 코드들을 생성

3. new FieldError() 를 생성하면서 메시지 코드들을 보관
4. th:erros 에서 메시지 코드들로 메시지를 순서대로 메시지에서 찾고, 노출



## 오류 코드와 메시지 처리6

### 스프링이 직접 만드는 오류 메시지 처리

item object의 price 필드에 문자열을 입력하면 다음과 같이 스프링이 생성한 기본 메시지가 출력된다.

``` 
ailed to convert property value of type java.lang.String to required type java.lang.Integer for property price; nested exception is java.lang.NumberFormatException: For input string: "A"
```



스프링은 직접 아래와 같은 메시지 코드를 추가한다.

* typeMismatch.item.price 
* typeMismatch.price 
* typeMismatch.java.lang.Integer 
* typeMismatch



이를 해결하기 위해 errors.properties에 이를 덮어쓰도록 한다.

``` properties
typeMismatch.java.lang.Integer=숫자를 입력해주세요. 
typeMismatch=타입 오류입니다.
```



## Validator 분리1

### ItemValidator

ItemValidator을 만들어 검증 로직을 분리하자.

``` java
package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class ItemValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return Item.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Item item = (Item) target;

    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "itemName", "required");

    if (item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
      errors.rejectValue("price", "range", new Object[]{1000, 1000000}, null);
    }

    if (item.getQuantity() == null || item.getQuantity() > 10000) {
      errors.rejectValue("quantity", "max", new Object[]{9999}, null);
    }

    //특정 필드 예외가 아닌 전체 예외
    if (item.getPrice() != null && item.getQuantity() != null) {
      int resultPrice = item.getPrice() * item.getQuantity();
      if (resultPrice < 10000) {
        errors.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
      }
    }
  }
}

```



스프링은 검증을 체계적으로 제공하기 위해 다음 인터페이스를 제공한다.

``` java
public interface Validator {
    boolean supports(Class<?> clazz);
    void validate(Object target, Errors errors);
}
```

* supports() {} : 해당 검증기를 지원하는 여부 확인(뒤에서 설명)
* validate(Object target, Errors errors) : 검증 대상 객체와BindingResult



Controller에서 아래와 같이 호출하면 된다.

``` java
private final ItemValidator itemValidator;

@PostMapping("/add")
public String addItemV5(@ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
  itemValidator.validate(item, bindingResult);
  
  // 성공 로직
}
```



## Validator 분리2

스프링 Validator 인터페이스를 사용하면 스프링의 추가적인 도움을 받을 수 있다.

### WebDataBinder

WebDataBinder는 스프링의 파라미터 바인딩의 역할을 해주고 검증 기능도 내부에 포함한다.

controller에 아래 코드와 @Validated를 추가하면 동일하게 동작한다.

``` java
@InitBinder
public void init(WebDataBinder dataBinder) {
  dataBinder.addValidators(itemValidator);
}

@PostMapping("/add")
public String addItemV6(@Validated @ModelAttribute Item item, BindingResult bindingResult, RedirectAttributes redirectAttributes) {}
```



### @Validated 

이는 검증기를 실행하라는 애노테이션이다. 여러 검증기의 supports()를 사용하여 적절한 검증기를 찾아낸다.

검증 시, @Validated와 @Valid 둘 다 사용가능하다. 

javax.validation.@Valid 를 사용하려면 build.gradle 의존관계 추가가 필요하다.

* implementation 'org.springframework.boot:spring-boot-starter-validation'

@Validated 는 스프링 전용 검증 애노테이션이고, @Valid 는 자바 표준 검증 애노테이션이다.



### 글로벌 설정

각각의 controller가 아닌 글로벌하게 validator를 설정할 수 있다.

``` java
import hello.itemservice.web.validation.ItemValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class ItemServiceApplication implements WebMvcConfigurer {

  public static void main(String[] args) {
    SpringApplication.run(ItemServiceApplication.class, args);
  }

  @Override
  public Validator getValidator() {
    return new ItemValidator();
  }
}
```

