package com.tcmatch.tcmatch.service;

import com.tcmatch.tcmatch.bot.exceptions.InsufficientFundsException;
import com.tcmatch.tcmatch.model.Order;
import com.tcmatch.tcmatch.model.Wallet;
import com.tcmatch.tcmatch.model.dto.WalletDto;
import com.tcmatch.tcmatch.repository.OrderRepository;
import com.tcmatch.tcmatch.repository.TransactionRepository;
import com.tcmatch.tcmatch.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository; // Нужен для обновления статуса Escrow в заказе
    private final TransactionRepository transactionRepository;

    // Комиссия платформы (10% - тестовая)
    private static final BigDecimal SERVICE_FEE_PERCENT = new BigDecimal("0.10");

    /**
     * Метод будет вызываться при создании нового пользователя.
     */
    @Transactional
    public Wallet initializeWallet(Long userChatId) {
        Wallet wallet = new Wallet();
        wallet.setUserChatId(userChatId);
        // Присваиваем 5000 руб. тестовых денег для удобства тестирования Escrow
        wallet.setBalance(new BigDecimal("200.00"));
        log.info("Инициализация: Создан кошелек для {} с тестовым балансом.", userChatId);
        return walletRepository.save(wallet);
    }

    /**
     * Пополнение баланса (имитация успешного платежа через ЮKassa).
     */
    @Transactional
    public Wallet deposit(Long userChatId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserChatId(userChatId)
                .orElseThrow(() -> new RuntimeException("Кошелек не найден для ChatId: " + userChatId));

        wallet.setBalance(wallet.getBalance().add(amount));
        log.info("Пополнение: Пользователь {} пополнил баланс на {}", userChatId, amount);
        return walletRepository.save(wallet);
    }

    /**
     * Шаг 1: Заморозка средств Заказчика под заказ (Escrow Hold).
     * Вызывается, когда Заказчик принимает отклик и начинает заказ.
     * @param orderId ID заказа
     */
    @Transactional
    public void holdFundsForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден ID: " + orderId));

        // 🔥 ВАЖНО: Приводим totalBudget к BigDecimal для точных расчетов
        BigDecimal amount = BigDecimal.valueOf(order.getTotalBudget());
        Long customerChatId = order.getCustomerChatId();

        Wallet customerWallet = walletRepository.findByUserChatId(customerChatId)
                .orElseThrow(() -> new RuntimeException("Кошелек заказчика не найден ChatId: " + customerChatId));

        if (customerWallet.getBalance().compareTo(amount) < 0) {
            // Используем кастомное исключение для обработки в контроллере
            throw new InsufficientFundsException("Недостаточно средств на балансе для заморозки: " + amount);
        }

        // Списываем с доступного баланса и добавляем в замороженный
        customerWallet.setBalance(customerWallet.getBalance().subtract(amount));
        customerWallet.setFrozenBalance(customerWallet.getFrozenBalance().add(amount));

        // Отмечаем заказ как имеющий замороженные средства
        order.setEscrowStatus(Order.EscrowStatus.FROZEN);

        walletRepository.save(customerWallet);
        orderRepository.save(order);

        log.info("ESCROW HOLD: Заморожены {} для заказа {} (Заказчик: {})", amount, orderId, customerChatId);
    }

    public WalletDto getWalletDto(Long chatId) {
        // Получаем кошелек пользователя
        Wallet wallet = walletRepository.findByUserChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Кошелёк пользователя " + chatId + " не найден"));

        // Используем данные из сущности Wallet
        return WalletDto.builder()
                .balance(wallet.getBalance())
                .frozenBalance(wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO)
                .build();
    }

    /**
     * Шаг 2: Выплата фрилансеру и списание комиссии (Escrow Release).
     * Вызывается, когда Заказчик подтверждает выполнение работы.
     * @param orderId ID заказа
     */
    @Transactional
    public void releaseFundsToFreelancer(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден ID: " + orderId));

        if (order.getEscrowStatus() != Order.EscrowStatus.FROZEN) {
            throw new RuntimeException("Ошибка: Средства по заказу не были заморожены.");
        }

        BigDecimal projectBudget = BigDecimal.valueOf(order.getTotalBudget());
        Long customerChatId = order.getCustomerChatId();
        Long freelancerChatId = order.getFreelancerChatId();

        // 1. Размораживаем средства у Заказчика (просто уменьшаем frozen_balance)
        Wallet customerWallet = walletRepository.findByUserChatId(customerChatId)
                .orElseThrow(() -> new RuntimeException("Кошелек заказчика не найден"));

        if (customerWallet.getFrozenBalance().compareTo(projectBudget) < 0) {
            throw new RuntimeException("Ошибка Escrow: Недостаточно замороженных средств для разморозки.");
        }
        customerWallet.setFrozenBalance(customerWallet.getFrozenBalance().subtract(projectBudget));

        // 2. Рассчитываем комиссию и сумму к выплате
        BigDecimal fee = projectBudget.multiply(SERVICE_FEE_PERCENT); // 10%
        BigDecimal payoutAmount = projectBudget.subtract(fee);

        // 3. Зачисляем средства на кошелек Фрилансера
        Wallet freelancerWallet = walletRepository.findByUserChatId(freelancerChatId)
                .orElseThrow(() -> new RuntimeException("Кошелек фрилансера не найден"));

        freelancerWallet.setBalance(freelancerWallet.getBalance().add(payoutAmount));

        // 4. Обновляем статус заказа
        order.setEscrowStatus(Order.EscrowStatus.RELEASED);

        walletRepository.save(customerWallet);
        walletRepository.save(freelancerWallet);
        orderRepository.save(order);

        log.info("ESCROW RELEASE: Заказ {} завершен. Выплачено фрилансеру {} (Комиссия: {})",
                orderId, payoutAmount, fee);
    }

    /**
     * Проверяет, достаточно ли средств на балансе
     * @return true если средств достаточно, false если недостаточно
     */
    public boolean hasSufficientFunds(Long chatId, BigDecimal requiredAmount) {
        Wallet wallet = walletRepository.findByUserChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Кошелек не найден"));

        return wallet.getBalance().compareTo(requiredAmount) >= 0;
    }

    /**
     * Проверяет и возвращает информацию о доступных средствах
     * @return информацию о балансе или выбрасывает исключение
     */
    public void validateSufficientFunds(Long chatId, BigDecimal requiredAmount) {
        Wallet wallet = walletRepository.findByUserChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Кошелек не найден"));

        if (wallet.getBalance().compareTo(requiredAmount) < 0) {
            throw new InsufficientFundsException(requiredAmount, wallet.getBalance());
        }
    }

    /**
     * Списание средств с баланса пользователя
     * Используется для покупок из баланса
     */
    @Transactional
    public void withdraw(Long chatId, BigDecimal amount) {
        log.info("Списание средств: chatId={}, amount={}", chatId, amount);

        // Проверяем входные параметры
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма списания должна быть больше нуля");
        }

        // Получаем кошелек пользователя
        Wallet wallet = walletRepository.findByUserChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Кошелек не найден для ChatId: " + chatId));

        // Проверяем достаточность средств
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Недостаточно средств на балансе. " +
                            "Требуется: " + amount + " ₽, " +
                            "доступно: " + wallet.getBalance() + " ₽",
                    amount,
                    wallet.getBalance()
            );
        }

        // Списываем средства
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Логируем операцию (можно создать запись в истории транзакций)
        log.info("Средства списаны: chatId={}, amount={}, новый баланс={}",
                chatId, amount, newBalance);

        // 🔥 МОЖНО СОЗДАТЬ ТРАНЗАКЦИЮ В ИСТОРИИ
        // transactionService.createWithdrawalTransaction(chatId, amount);
    }

    /**
     * Полный возврат средств (например, при отмене операции)
     */
    @Transactional
    public void refund(Long chatId, BigDecimal amount) {
        log.info("Возврат средств: chatId={}, amount={}", chatId, amount);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма возврата должна быть больше нуля");
        }

        Wallet wallet = walletRepository.findByUserChatId(chatId)
                .orElseThrow(() -> new RuntimeException("Кошелек не найден"));

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        log.info("Средства возвращены: chatId={}, amount={}, новый баланс={}",
                chatId, amount, newBalance);

        // 🔥 МОЖНО СОЗДАТЬ ЗАПИСЬ О ВОЗВРАТЕ
        // transactionService.createRefundTransaction(chatId, amount);
    }
}