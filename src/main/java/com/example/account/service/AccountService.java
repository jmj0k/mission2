package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.account.type.AccountStatus.IN_USE;


@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    @Transactional
    public Account getAccount(Long id) {
        if(id < 0){
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }


    /**
     * User 조회 =>
     * Account 생성 =>
     * Account 저장 =>
     * 정보 넘김
     */
    // 계좌 생성 메소드
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        /**
         * accountuser가 없으면 throw,
         * 있으면 accountUser에 값을 넘겨줌
         */
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        // 유저의 계좌 수 체크
        validateCreateAccount(accountUser);

        // 계좌 번호 생성, 아무것도 없을 경우 1--, 있다면 + 1 => 랜덤 생성으로 변경 필요
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc()
                .map(account -> (Integer.parseInt(account.getAccountNumber())) + 1 + "")
                .orElse("1000000000");

        return AccountDto.fromEntity(
                accountRepository.save(Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(IN_USE)
                        .accountNumber(newAccountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build())
        );
    }

    // 유저 계좌 수 제한 (MAXIMUM: 10)
    private void validateCreateAccount(AccountUser accountUser) {
        if(accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(ErrorCode.MAX_ACCOUNT_PER_USER_10);
        }
    }
    
    // 유저 계좌 해지 유효 체크
    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        // 유저를 찾을 수 없을 경우
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        // 계좌를 찾을 수 없을 경우
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 유효 체크 메소드 (소유주, 해지 여부, 잔액 체크)
        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(AccountStatus.UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        // TEST 용도
        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    // 유효 체크 메소드
    private void validateDeleteAccount(AccountUser accountUser, Account account) {
        // 계좌 소유주와 다를 경우
        if(!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UN_MATCH);
        }
        // 이미 해지된 계좌일 경우
        if(account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }
        // 계좌 잔액이 남아있을 경우
        if(account.getBalance() > 0) {
            throw new AccountException(ErrorCode.BALANCE_NOT_EMPTY);
        }
    }
}
