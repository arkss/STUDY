# 01. 회원 등록 API

``` java
package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PostMapping("api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }
    

    @Data
    static class CreateMemberRequest {
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}
```



### V1의 문제점

V1에서는 엔티티가 Request Body에 직접 매핑된다. 이는 아래와 같은 문제가 있다.

* 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다. 
* 엔티티에 API 검증을 위한 로직이 들어간다. (@NotEmpty 등)
* 실무에서는 회원 엔티티를 위한 API가 다양하게 만들어지는데 한 엔티티에 각각의 API를 위한 모든 요청 요구사항을 담기는 어렵다.
* 엔티티가 변경되면 API 스펙이 변한다.



# 02. 회원 수정 API

``` java
@PatchMapping("api/v2/members/{id}")
public UpdateMemberResponse updateMemberV2(
        @PathVariable("id") Long id,
        @RequestBody @Valid UpdateMemberRequest request) {
    memberService.update(id, request.getName());
    Member findMember = memberService.findOne(id);
    return new UpdateMemberResponse(findMember.getId(), findMember.getName());
}

@Data
static class UpdateMemberRequest {
    private String name;
}

@Data
@AllArgsConstructor
static class UpdateMemberResponse {
    private Long id;
    private String name;
}
```

``` java
@Transactional
public void update(Long id, String name) {
    Member member = memberRepository.findOne(id);
    member.setName(name);
}
```

회원 등록과 마찬가지로 DTO를 사용하여 요청과 응답을 처리한다.



# 03. 회원 조회 API

``` java
@GetMapping("/api/v1/members")
public List<Member> membersV1() {
    return memberService.findMembers();
}
```

회원 등록 API V1과 같은 이유로 응답값으로 엔티티를 사용하면 문제가 생긴다.



``` java
@GetMapping("/api/v2/members")
public Result membersV2() {
    List<Member> findMembers = memberService.findMembers();

    List<MemberDto> collect = findMembers.stream()
            .map(m -> new MemberDto(m.getName()))
            .collect(Collectors.toList());

    return new Result(collect);
}

@Data
@AllArgsConstructor
static class Result<T> {
    private T data;
}

@Data
@AllArgsConstructor
static class MemberDto {
    private String name;
}
```

엔티티 대신 DTO를 사용하여 문제를 해결한다.

추가로 Result 클래스로 컬렉션을 감싸서 향후 필요한 필드를 유연하게 추가할 수 있다. 만약 그냥 collect를 반환한다면 배열 형태의 Json이 반환되어 전체 멤버 수 등의 필드를 추가하기가 어려워진다.