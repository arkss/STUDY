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

## 오류 코드와 메시지 처리3

## 오류 코드와 메시지 처리4

## 오류 코드와 메시지 처리5

## 오류 코드와 메시지 처리6

## Validator 분리1

## Validator 분리2