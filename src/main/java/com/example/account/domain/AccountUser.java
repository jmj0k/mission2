package com.example.account.domain;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder // 상속 구조에서 빌더를 간단히 사용하기 위해 슈퍼빌더를 사용했습니다.
/*https://projectlombok.org/features/experimental/SuperBuilder */
public class AccountUser extends AccountBaseEntity {
    private String name;
}
