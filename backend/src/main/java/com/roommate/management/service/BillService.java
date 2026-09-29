package com.roommate.management.service;

import com.roommate.management.dto.request.BillCreateRequest;
import com.roommate.management.dto.request.BillUpdateRequest;
import com.roommate.management.dto.response.BillResponse;
import com.roommate.management.entity.Bill;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.BillRepository;
import com.roommate.management.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public BillResponse create(Long userId, BillCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_BILL);

        RoomMember paidBy = null;
        if (request.paidByMemberId() != null) {
            paidBy = roomMemberRepository.findById(request.paidByMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            roomAccessService.requireSameRoom(caller, paidBy.getRoom().getId());
        }

        Bill bill = Bill.builder()
                .room(caller.getRoom())
                .title(request.title())
                .amount(request.amount())
                .dueDate(request.dueDate())
                .paidBy(paidBy)
                .category(request.category())
                .status(BillStatus.PENDING)
                .createdBy(caller)
                .build();
        bill = billRepository.save(bill);

        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.BILL, "Bill created", bill.getId(),
                caller.getUser().getFullName() + " added bill \"" + bill.getTitle() + "\"");

        return toResponse(bill);
    }

    @Transactional(readOnly = true)
    public List<BillResponse> list(Long userId, BillStatus status) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        List<Bill> bills = status == null
                ? billRepository.findByRoomIdOrderByDueDateAsc(caller.getRoom().getId())
                : billRepository.findByRoomIdAndStatus(caller.getRoom().getId(), status);
        return bills.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BillResponse update(Long userId, Long billId, BillUpdateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_BILL);
        Bill bill = billRepository.findById(billId).orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        roomAccessService.requireSameRoom(caller, bill.getRoom().getId());

        if (request.title() != null) bill.setTitle(request.title());
        if (request.amount() != null) bill.setAmount(request.amount());
        if (request.dueDate() != null) bill.setDueDate(request.dueDate());
        if (request.category() != null) bill.setCategory(request.category());
        if (request.amountPaid() != null) {
            if (request.amountPaid().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Amount paid cannot be negative");
            }
            if (request.amountPaid().compareTo(bill.getAmount()) > 0) {
                throw new BadRequestException("Amount paid cannot exceed the bill amount");
            }
            bill.setAmountPaid(request.amountPaid());
        }
        if (request.paidByMemberId() != null) {
            RoomMember paidBy = roomMemberRepository.findById(request.paidByMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
            roomAccessService.requireSameRoom(caller, paidBy.getRoom().getId());
            bill.setPaidBy(paidBy);
        }

        if (request.status() != null) {
            bill.setStatus(request.status());
        } else if (bill.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            bill.setStatus(bill.getAmountPaid().compareTo(bill.getAmount()) >= 0 ? BillStatus.PAID : BillStatus.PARTIALLY_PAID);
        }

        bill = billRepository.save(bill);
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.BILL, "Bill updated", bill.getId(),
                caller.getUser().getFullName() + " updated bill \"" + bill.getTitle() + "\"");
        return toResponse(bill);
    }

    @Transactional
    public void delete(Long userId, Long billId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_BILL);
        Bill bill = billRepository.findById(billId).orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        roomAccessService.requireSameRoom(caller, bill.getRoom().getId());
        billRepository.delete(bill);
    }

    @Transactional
    public String uploadPhoto(Long userId, Long billId, MultipartFile file) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_BILL);
        Bill bill = billRepository.findById(billId).orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        roomAccessService.requireSameRoom(caller, bill.getRoom().getId());
        String path = fileStorageService.store(file, "bills", List.of("image/jpeg", "image/png", "image/jpg", "application/pdf"));
        bill.setBillPhotoUrl(fileStorageService.toPublicUrl(path));
        billRepository.save(bill);
        return bill.getBillPhotoUrl();
    }

    /** Called by the scheduler to flag overdue bills. */
    @Transactional
    public void markOverdue(java.time.LocalDate today) {
        List<Bill> overdue = billRepository.findByStatusNotAndDueDateBefore(BillStatus.PAID, today);
        for (Bill bill : overdue) {
            if (bill.getStatus() != BillStatus.OVERDUE) {
                bill.setStatus(BillStatus.OVERDUE);
                billRepository.save(bill);
            }
        }
    }

    private BillResponse toResponse(Bill b) {
        return new BillResponse(
                b.getId(), b.getTitle(), b.getAmount(), b.getAmountPaid(), b.getDueDate(),
                b.getPaidBy() != null ? b.getPaidBy().getUser().getFullName() : null,
                b.getCategory().name(), b.getBillPhotoUrl(), b.getStatus().name()
        );
    }
}
