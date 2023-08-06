# 05. 검증2 - Bean Validation

## Bean Validation - 소개

### Bean Validation이란?

먼저 Bean Validation은 특정한 구현체가 아니라 Bean Validation 2.0(JSR-380)이라는 기술 표준이다. 쉽게 이야기해서 검증 애노테이션과 여러 인터페이스의 모음이다. 마치 JPA가 표준 기술이고 그 구현체로 하이버네이트가 있는 것과 같다.

Bean Validation을 구현한 기술중에 일반적으로 사용하는 구현체는 하이버네이트 Validator이다. 이름이 하이버네이트가 붙어서 그렇지 ORM과는 관련이 없다.



### **하이버네이트** Validator 관련 링크

* 공식 사이트: http://hibernate.org/validator/
* 공식 메뉴얼: https://docs.jboss.org/hibernate/validator/6.2/reference/en-US/html_single/ 
* 검증 애노테이션 모음: https://docs.jboss.org/hibernate/validator/6.2/reference/en-US/html_single/#validator-defineconstraints-spec





Bean Validation을 사용하면 애노테이션 하나로 검증 로직을 매우 편리하게 적용할 수 있다.

``` java
package hello.itemservice.domain.item;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class Item {

    private Long id;

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;

    @NotNull
    @Max(9999)
    private Integer quantity;

    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}

```



## Bean Validation - 시작

### Bean Validation 의존관계 추가

``` gradle
implementation 'org.springframework.boot:spring-boot-starter-validation'
```



### Bean Validation 적용

``` java
package hello.itemservice.domain.item;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class Item {

    private Long id;

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;

    @NotNull
    @Max(9999)
    private Integer quantity;

    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }
}

```

* javax.validation으로 시작하면 특정 구현에 관계없이 제공되는  표준 인터페이스이다.
* org.hiberate.validator로 시작하면 하이버네이트 validator 구현체를 사용할 때만 제공되는 검증기능이다. 하지만 실무에서 대부분 하이버네이트 validator를 사용하므로 자유롭게 사용해도 된다.



### 테스트 코드 검증

``` java
package hello.itemservice.validation;

import hello.itemservice.domain.item.Item;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

public class BeanValidationTest {

  @Test
  void beanValidation() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    Item item = new Item();
    item.setItemName(" "); //공백
    item.setPrice(0);
    item.setQuantity(10000);

    Set<ConstraintViolation<Item>> violations = validator.validate(item);
    for (ConstraintViolation<Item> violation : violations) {
      System.out.println("violation = " + violation);
      System.out.println("violation.message = " + violation.getMessage());
    }
  }

}
```

``` 
violation = ConstraintViolationImpl{interpolatedMessage='공백일 수 없습니다', propertyPath=itemName, rootBeanClass=class hello.itemservice.domain.item.Item, messageTemplate='{javax.validation.constraints.NotBlank.message}'}
violation.message = 공백일 수 없습니다
violation = ConstraintViolationImpl{interpolatedMessage='9999 이하여야 합니다', propertyPath=quantity, rootBeanClass=class hello.itemservice.domain.item.Item, messageTemplate='{javax.validation.constraints.Max.message}'}
violation.message = 9999 이하여야 합니다
violation = ConstraintViolationImpl{interpolatedMessage='1000에서 1000000 사이여야 합니다', propertyPath=price, rootBeanClass=class hello.itemservice.domain.item.Item, messageTemplate='{org.hibernate.validator.constraints.Range.message}'}
violation.message = 1000에서 1000000 사이여야 합니다
```





## Bean Validation - 프로젝트 준비 V3

### ValidationItemControllerV3 **컨트롤러 생성** 

* hello.itemservice.web.validation.ValidationItemControllerV2 복사

* hello.itemservice.web.validation.ValidationItemControllerV3 붙여넣기 
* URL 경로 변경: validation/v2/ validation/v3/



### 템플릿 파일 복사

* validation/v2 디렉토리의 모든 템플릿 파일을 validation/v3 디렉토리로 복사
    * /resources/templates/validation/v2/ -> /resources/templates/validation/v3/

```
addForm.html
editForm.html
item.html
items.html
```

* /resources/templates/validation/v3/ 하위 4개 파일 모두 URL 경로 변경
    * validation/v2/ -> validation/v3/

```
addForm.html
editForm.html
item.html
items.html
```



## Bean Validation - 스프링 적용

### 스프링 MVC에서의 Bean Validator

스프링 부트가 spring-boot-starter-validation 라이브러리를 넣으면 자동으로 Bean Validator를 인지하고 스프링에 통합한다.

따라서 @Valid, @Validated가 있으면 애노테이션 기반으로 검증을 수행한다.

검증 오류가 발생하면 FieldError, ObjectError를 생성해서 BindingResult에 담아준다.



### 검증 순서

1. @ModelAttribute 각각의 필드에 타입 변환 시도
    1. 성공하면 다음으로
    2. 실패하면 typeMismatch로 FieldError 추가
2. Validator 적용



### 바인딩에 성공한 필드만 Bean Validation 적용

BeanValidator는 바인딩에 실패한 필드는 BeanValidation을 적용하지 않는다.



## Bean Validation - 에러 코드

### 에러 코드 변경

Bean Validation이 기본으로 제공하는 오류 메시지를 변경해보자.

Bean Validation을 적용하고 bindingResult에 등록된 검증 오류 코드를 보면, 오류 코드가 애노테이션 이름으로 등록된다.



* @NotBlank
    * NotBlank.item.itemName
    * NotBlank.itemName
    * NotBlank.java.lang.String
    * NotBlank
* @Range
    * Range.item.price
    * Range.price
    * Range.java.lang.Integer
    * Range



### 메시지 등록

errors.properties

``` properties
NotBlank={0} 공백X 
Range={0}, {2} ~ {1} 허용 
Max={0}, 최대 {1}
```

{0}은 필드명이고, {1}, {2}는 각 애노테이션 마다 다르다.



### Bean Validation 메시지 찾는 순서

1. 생성된 메시 코드 순서대로 messageSource에서 메시지 찾기(errors.properties 기반)
2. 애노테이션의 message 속성 사용
3. 라이브러리가 제공하는 기본 값 사용



## Bean Validation - 오브젝트 오류

### @ScriptAssert()

오브젝트 오류는 @ScriptAssert()를 통해 해결할 수 있다.

``` java
@Data
@ScriptAssert(lang = "javascript", script = "_this.price * _this.quantity >= 10000")
public class Item {
  //...
}
```



그 결과 생성되는 메시지 코드는 다음과 같다.

* ScriptAssert.item
* ScriptAssert



하지만 실제 사용해보면 제약이 많고 복잡하다. 그리고 실무에서는 검증 기능이 해당 객체의 범위를 넘어서는 경우들도 종종 등장하는데, 그런 경우 대응이 어렵다.

따라서 오브젝트 오류의 경우에는 기존과 같이 직접 자바 코드로 작성하는 걸 권장한다.

``` java
if (item.getPrice() != null && item.getQuantity() != null) {
  int resultPrice = item.getPrice() * item.getQuantity();
  if (resultPrice < 10000) {
    bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
  }
}
```





## Bean Validation - 수정에 적용

### ValidationItemControllerV3 - edit() 변경

``` java
@PostMapping("/{itemId}/edit")
public String edit(@PathVariable Long itemId, @Validated @ModelAttribute Item item, BindingResult bindingResult) {
  if (item.getPrice() != null && item.getQuantity() != null) {
    int resultPrice = item.getPrice() * item.getQuantity();
    if (resultPrice < 10000) {
      bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
    }
  }

  if (bindingResult.hasErrors()) {
    log.info("errors={}", bindingResult);
    return "validation/v3/editForm";
  }

  itemRepository.update(itemId, item);
  return "redirect:/validation/v3/items/{itemId}";
}
```



적절하게 html 파일도 변경해주어야 한다.



## Bean Validation - 한계

데이터의 저장과 수정의 validation 요구사항이 다른 경우, 현재는 구현이 불가능하다.



## Bean Validation - groups

동일한 모델 객체를 등록할 때와 수정할 때 각각 다르게 검증하는 방법을 알아보자.

* BeanValidation의 groups 기능 사용
* Item을 직접 사용하지 않고, ItemSaveForm, ItemUpdateForm 같은 폼 전송을 위한 별도의 모델 객체를 만들어서 사용



### groups 적용

#### 저장용 groups 생성

``` java
package hello.itemservice.domain.item;
public interface SaveCheck {
}
```



#### 수정용 groups 생성

``` java
package hello.itemservice.domain.item;
public interface UpdateCheck {
}
```



#### item 수정

``` java
package hello.itemservice.domain.item;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class Item {


  @NotNull(groups = UpdateCheck.class) //수정시에만 적용
  private Long id;
  
  @NotBlank(groups = {SaveCheck.class, UpdateCheck.class})
  private String itemName;
  
  @NotNull(groups = {SaveCheck.class, UpdateCheck.class})
  @Range(min = 1000, max = 1000000, groups = {SaveCheck.class, UpdateCheck.class})
  private Integer price;

  @NotNull(groups = {SaveCheck.class, UpdateCheck.class})
  @Max(value = 9999, groups = SaveCheck.class) //등록시에만 적용
private Integer quantity;

  public Item() {
  }

  public Item(String itemName, Integer price, Integer quantity) {
    this.itemName = itemName;
    this.price = price;
    this.quantity = quantity;
  }
}
```



#### controller 수정

``` java
@PostMapping("/add")
public String addItemV2(@Validated(SaveCheck.class) @ModelAttribute Item item,
                        BindingResult bindingResult, RedirectAttributes redirectAttributes) {
  //...
}

@PostMapping("/{itemId}/edit")
public String editV2(@PathVariable Long itemId, @Validated(UpdateCheck.class)
                     @ModelAttribute Item item, BindingResult bindingResult) {
  //...
}
```

* @Valid는 groups 기능이 존재하지 않는다.



### 정리

groups 기능을 사용해서 등록과 수정시에 각각 다르게 검증을 할 수 있었다. 그런데 groups 기능을 사용하니 Item 은 물론이고, 전반적으로 복잡도가 올라갔다.

사실 groups 기능은 실제 잘 사용되지는 않는데, 그 이유는 실무에서는 주로 다음에 등장하는 등록용 폼 객체와 수정용 폼 객체를 분리해서 사용하기 때문이다.



## Form 전송 객체 분리 - 프로젝트 준비 V4

### ValidationItemControllerV4 **컨트롤러 생성** 

* hello.itemservice.web.validation.ValidationItemControllerV3 복사

* hello.itemservice.web.validation.ValidationItemControllerV4 붙여넣기 
* URL 경로 변경: validation/v3/ validation/v4/



### 템플릿 파일 복사

* validation/v3 디렉토리의 모든 템플릿 파일을 validation/v4 디렉토리로 복사
    * /resources/templates/validation/v3/ -> /resources/templates/validation/v4/

```
addForm.html
editForm.html
item.html
items.html
```

* /resources/templates/validation/v4/ 하위 4개 파일 모두 URL 경로 변경
    * validation/v3/ -> validation/v4/

```
addForm.html
editForm.html
item.html
items.html
```





## Form 전송 객체 분리 - 소개

실무에서는 groups 를 잘 사용하지 않는데, 그 이유가 다른 곳에 있다. 바로 등록시 폼에서 전달하는 데이터가 Item 도메인 객체와 딱 맞지 않기 때문이다. Item을 만들기 위해 데이터를 사용자로부터 받는 것 뿐 아니라 DB 등에서 조회해서 만들어야 할 수 도 있다.

그래서 보통 Item 을 직접 전달받는 것이 아니라, 복잡한 폼의 데이터를 컨트롤러까지 전달할 별도의 객체를 만들어서 전달한다. 예를 들면 ItemSaveForm 이라는 폼을 전달받는 전용 객체를 만들어서 @ModelAttribute 로 사용한다. 이것을 통해 컨트롤러에서 폼 데이터를 전달 받고, 이후 컨트롤러에서 필요한 데이터를 사용해서 Item 을 생성한다.



### 폼 데이터 전달에 Item 도메인 객체 사용

* HTML Form -> Item -> Controller -> Item -> Repository
* 장점 : 중간에 Item을 만드는 과정이 없어서 간단하다.
* 단점 : 간단한 경우에만 적용할 수 있다. 수정 시 검증이 중복 될 수 있고, groups를 사용해야 한다.



### 폼 데이터 전달을 위한 별도의 객체 사용

* HTML Form -> ItemSaveForm -> Controller -> Item 생성 -> Repository
* 장점 : 전송하는 폼 데이터가 복잡해도 거기에 맞춘 별도의 폼 객체를 사용해서 데이터를 전달 받을 수 있다. 보통 등록과, 수정용으로 별도의 폼 객체를 만들기 때문에 검증이 중복되지 않는다.
* 단점 : 폼 데이터를 기반으로 컨트롤러에서 Item 객체를 생성하는 변환 과정이 추가된다.



### 폼 데이터 전달을 위한 별도 객체의 이름

ItemSave, ItemSaveForm, ItemSaveRequest, ItemSaveDto 등 다양하게 사용해도 되지만 일관성이 유지되어야 한다.



## Form 전송 객체 분리 - 개발

### Item 원복

``` java
package hello.itemservice.domain.item;

import lombok.Data;

@Data
public class Item {

  private Long id;
  private String itemName;
  private Integer price;
  private Integer quantity;

  public Item() {
  }

  public Item(String itemName, Integer price, Integer quantity) {
    this.itemName = itemName;
    this.price = price;
    this.quantity = quantity;
  }
}

```



### ItemSaveForm

``` java
package hello.itemservice.web.validation.form;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

@Data
public class ItemSaveForm {

  @NotBlank
  private String itemName;
  @NotNull
  @Range(min = 1000, max = 1000000)
  private Integer price;
  @NotNull
  @Max(value = 9999)
  private Integer quantity;
}

```



### ValidationItemControllerV4

``` java
  @PostMapping("/add")
  public String addItem(@Validated @ModelAttribute("item") ItemSaveForm form, BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {

    // 특정 필드가 아닌 복합 룰 검증
    if (form.getPrice() != null && form.getQuantity() != null) {
      int resultPrice = form.getPrice() * form.getQuantity();
      if (resultPrice < 10000) {
        bindingResult.reject("totalPriceMin", new Object[]{10000, resultPrice}, null);
      }
    }

    // 검증에 실패하면 다시 입력 폼으로
    if (bindingResult.hasErrors()) {
      log.info("errors={}", bindingResult);
      return "validation/v4/addForm";
    }

    Item item = new Item();
    item.setItemName(form.getItemName());
    item.setPrice(form.getPrice());
    item.setQuantity(form.getQuantity());

    Item savedItem = itemRepository.save(item);
    redirectAttributes.addAttribute("itemId", savedItem.getId());
    redirectAttributes.addAttribute("status", true);
    return "redirect:/validation/v4/items/{itemId}";
  }
```



수정에 대해서도 위와 동일한 방식으로 진행한다.





## Bean Validation - HTTP 메시지 컨버터

@Valid, @Validated는 HttpMessageConverter(@RequestBody)에도 적용할 수 있다.



### ValidationItemApiController

``` java
package hello.itemservice.web.validation;

import hello.itemservice.web.validation.form.ItemSaveForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/validation/api/items")
public class ValidationItemApiController {

  @PostMapping("/add")
  public Object addItem(@Validated @RequestBody ItemSaveForm form, BindingResult bindingResult) {
    log.info("API 컨트롤러 호출");

    if (bindingResult.hasErrors()) {
      log.info("검증 오류 발생 errors={}", bindingResult);
      return bindingResult.getAllErrors();
    }
    log.info("성공 로직 실행");
    return form;
  }
}

```



API의 경우 3가지 경우를 나누어 생각해야 한다.

* 성공 요청 : 성공
* 실패 요청 : JSON을 객체로 생성하는 것 자체가 실패함
* 검증 오류 요청 : JSON을 객체로 생성하는 것은 성공했고, 검증에서 실패함



### 실패 요청

price의 값에 숫자가 아닌 문자를 전달하여 실패하는 경우

``` json
{
   "timestamp":"2021-04-20T00:00:00.000+00:00",
   "status":400,
   "error":"Bad Request",
   "message":"",
   "path":"/validation/api/items/add"
}
```



HttpMessageConverter에서 요청 JSON을 ItemSaveForm 객체로 생성하는데 실패한다.

이 경우는 ItemSaveForm 객체를 만들지 못하기 때문에 컨트롤러 자체가 호출되지 않고 그 전에 예외가 발생한다.

물론 Validator도 실행되지 않는다.



### 검증 오류 요청

HttpMessageConverter는 성공하지만 검증에서 오류가 발생하는 경우를 확인해보자.

quantity를 1000으로 요청해 @Max(9999)에서 걸리도록 한다.

``` java
[
   {
      "codes":[
         "Max.itemSaveForm.quantity",
         "Max.quantity",
         "Max.java.lang.Integer",
         "Max"
      ],
      "arguments":[
         {
            "codes":[
               "itemSaveForm.quantity",
               "quantity"
            ],
            "arguments":null,
            "defaultMessage":"quantity",
            "code":"quantity"
         },
         9999
      ],
      "defaultMessage":"9999 이하여야 합니다",
      "objectName":"itemSaveForm",
      "field":"quantity",
      "rejectedValue":10000,
      "bindingFailure":false,
      "code":"Max"
   }
]
```



### @ModelAttribute vs @RequestBody

* @ModelAttribute 는 필드 단위로 정교하게 바인딩이 적용된다. 특정 필드가 바인딩 되지 않아도 나머지 필드는 정상 바인딩 되고, Validator를 사용한 검증도 적용할 수 있다.
* @RequestBody 는 HttpMessageConverter 단계에서 JSON 데이터를 객체로 변경하지 못하면 이후 단계 자체가 진행되지 않고 예외가 발생한다. 컨트롤러도 호출되지 않고, Validator도 적용할 수 없다.