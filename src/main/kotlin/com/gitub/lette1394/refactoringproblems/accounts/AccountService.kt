package com.gitub.lette1394.refactoringproblems.accounts

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class AccountService(
    private val accountRepository: AccountRepository,
    private val accountNotificationApi: AccountNotificationApi,
) {
    private val log = KotlinLogging.logger {}

    @Transactional
    fun createAccount(): Account {
        val accountEntity = accountRepository.save(
            AccountEntity(
                isVerified = false,
                isClosed = false,
                isFrozen = false,
                balance = BigDecimal.ZERO,
            )
        )

        return Account(
            accountId = accountEntity.id,
            isVerified = accountEntity.isVerified,
            isClosed = accountEntity.isClosed,
            isFrozen = accountEntity.isFrozen,
            balance = accountEntity.balance,
        )
    }

    @Transactional
    fun getAccount(accountId: Long): Account {
        val accountEntity = accountRepository.findById(accountId)
            .orElseThrow { throw RuntimeException("Account not found") }

        return Account(
            accountId = accountId,
            isVerified = accountEntity.isVerified,
            isClosed = accountEntity.isClosed,
            isFrozen = accountEntity.isFrozen,
            balance = accountEntity.balance,
        )
    }

    @Transactional
    fun holderVerified(accountId: Long) {
        val accountEntity = accountRepository.findById(accountId)
            .orElseThrow { throw RuntimeException("Account not found") }

        accountEntity.isVerified = true
    }

    @Transactional
    fun closeAccount(accountId: Long) {
        val accountEntity = accountRepository.findById(accountId)
            .orElseThrow { throw RuntimeException("Account not found") }

        accountEntity.isClosed = true
    }

    @Transactional
    fun freezeAccount(accountId: Long) {
        val accountEntity = accountRepository.findById(accountId)
            .orElseThrow { throw RuntimeException("Account not found") }

        var accountState = when(accountEntity.isVerified) {
            true -> Verified()
            false -> InVerified()
        }

        if (accountEntity.isClosed) {
            accountState = accountState.close{ }
        }

        accountState.freeze {
            accountEntity.isFrozen = true
            accountNotificationApi.notifyChangedToFrozen(
                AccountFrozenChangedRequest(
                    accountId = accountId,
                    isFrozen = true,
                ),
            )
        }
    }

    @Transactional
    fun deposit(accountId: Long, amount: BigDecimal) {
        val accountEntity = accountRepository.findById(accountId)
            .orElseThrow { throw RuntimeException("Account not found") }

        if (accountEntity.isClosed) {
            log.info { "계좌가 이미 닫혀있습니다. 입금할 수 없습니다. 요청을 무시합니다." }
            return
        }
        if (accountEntity.isFrozen) {
            accountEntity.isFrozen = false
            accountNotificationApi.notifyChangedToFrozen(
                AccountFrozenChangedRequest(
                    accountId = accountId,
                    isFrozen = false,
                ),
            )
        }
        accountEntity.balance = accountEntity.balance.add(amount)
    }

    @Transactional
    fun withdraw(accountId: Long, amount: BigDecimal) {
        val accountEntity = accountRepository.findById(accountId)
            .orElseThrow { throw RuntimeException("Account not found") }

        if (!accountEntity.isVerified) {
            log.info { "확인되지 않은 계좌는 출금할 수 없습니다. 요청을 무시합니다." }
            return
        }
        if (accountEntity.isClosed) {
            log.info { "계좌가 이미 닫혀있습니다. 출금할 수 없습니다. 요청을 무시합니다." }
            return
        }
        if (accountEntity.isFrozen) {
            accountEntity.isFrozen = false
            accountNotificationApi.notifyChangedToFrozen(
                AccountFrozenChangedRequest(
                    accountId = accountId,
                    isFrozen = false,
                ),
            )
        }
        val subtracted = accountEntity.balance.subtract(amount)
        if (subtracted < BigDecimal.ZERO) {
            log.info { "잔액이 부족합니다. 출금할 수 없습니다." }
            throw IllegalArgumentException("잔액이 부족합니다. 출금할 수 없습니다.")
        }
        accountEntity.balance = subtracted
    }
}
