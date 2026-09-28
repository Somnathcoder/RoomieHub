package com.roommate.management.service;

import com.roommate.management.dto.request.ShoppingItemCreateRequest;
import com.roommate.management.dto.request.ShoppingItemUpdateRequest;
import com.roommate.management.dto.request.ShoppingToExpenseRequest;
import com.roommate.management.dto.response.ExpenseResponse;
import com.roommate.management.dto.response.ShoppingItemResponse;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.ShoppingItem;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.BadRequestException;
import com.roommate.management.exception.ForbiddenException;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.ShoppingItemRepository;
import com.roommate.management.dto.request.ExpenseCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingService {

    private final ShoppingItemRepository shoppingItemRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;
    private final ExpenseService expenseService;

    @Transactional
    public ShoppingItemResponse addItem(Long userId, ShoppingItemCreateRequest request) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        ShoppingItem item = ShoppingItem.builder()
                .room(member.getRoom())
                .itemName(request.itemName())
                .quantity(request.quantity())
                .estimatedAmount(request.estimatedAmount())
                .addedBy(member)
                .status(ShoppingStatus.PENDING)
                .build();
        item = shoppingItemRepository.save(item);
        activityLogService.log(member.getRoom(), member.getUser(), ActivityModule.GROCERY, "Grocery item added", item.getId(),
                member.getUser().getFullName() + " added \"" + item.getItemName() + "\" to the shopping list");
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ShoppingItemResponse> list(Long userId, ShoppingStatus status) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        List<ShoppingItem> items = status == null
                ? shoppingItemRepository.findByRoomIdOrderByCreatedAtDesc(member.getRoom().getId())
                : shoppingItemRepository.findByRoomIdAndStatus(member.getRoom().getId(), status);
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShoppingItemResponse update(Long userId, Long itemId, ShoppingItemUpdateRequest request) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found"));
        roomAccessService.requireSameRoom(member, item.getRoom().getId());

        if (request.itemName() != null) item.setItemName(request.itemName());
        if (request.quantity() != null) item.setQuantity(request.quantity());
        if (request.estimatedAmount() != null) item.setEstimatedAmount(request.estimatedAmount());
        if (request.status() != null) item.setStatus(request.status());
        if (request.purchaseDate() != null) item.setPurchaseDate(request.purchaseDate());
        else if (request.status() == ShoppingStatus.PURCHASED && item.getPurchaseDate() == null) item.setPurchaseDate(LocalDate.now());

        return toResponse(shoppingItemRepository.save(item));
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found"));
        roomAccessService.requireSameRoom(member, item.getRoom().getId());
        shoppingItemRepository.delete(item);
    }

    @Transactional
    public String uploadItemPhoto(Long userId, Long itemId, MultipartFile file) {
        return uploadPhoto(userId, itemId, file, true);
    }

    @Transactional
    public String uploadBillPhoto(Long userId, Long itemId, MultipartFile file) {
        return uploadPhoto(userId, itemId, file, false);
    }

    private String uploadPhoto(Long userId, Long itemId, MultipartFile file, boolean isItemPhoto) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found"));
        roomAccessService.requireSameRoom(member, item.getRoom().getId());
        String path = fileStorageService.store(file, "grocery", List.of("image/jpeg", "image/png", "image/jpg", "application/pdf"));
        String url = fileStorageService.toPublicUrl(path);
        if (isItemPhoto) item.setItemPhotoUrl(url); else item.setBillPhotoUrl(url);
        shoppingItemRepository.save(item);
        return url;
    }

    @Transactional
    public ExpenseResponse convertToExpense(Long userId, Long itemId, ShoppingToExpenseRequest request) {
        RoomMember member = roomAccessService.getActiveMembership(userId);
        ShoppingItem item = shoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found"));
        roomAccessService.requireSameRoom(member, item.getRoom().getId());
        if (item.getStatus() != ShoppingStatus.PURCHASED) {
            throw new BadRequestException("Only a purchased item can be converted into an expense");
        }

        ExpenseCreateRequest expenseRequest = new ExpenseCreateRequest(
                item.getItemName(), "Created from shopping list item", request.actualAmount(),
                ExpenseCategory.GROCERY, request.paidByMemberId(), LocalDate.now(),
                request.splitType(), request.customSplits(), request.equalSplitMemberIds()
        );
        ExpenseResponse expense = expenseService.createExpense(userId, expenseRequest);

        activityLogService.log(member.getRoom(), member.getUser(), ActivityModule.GROCERY, "Shopping item converted to expense", item.getId(),
                member.getUser().getFullName() + " converted \"" + item.getItemName() + "\" into an expense");

        return expense;
    }

    private ShoppingItemResponse toResponse(ShoppingItem i) {
        return new ShoppingItemResponse(
                i.getId(), i.getItemName(), i.getQuantity(), i.getEstimatedAmount(),
                i.getAddedBy().getUser().getFullName(), i.getStatus().name(),
                i.getItemPhotoUrl(), i.getBillPhotoUrl(), i.getPurchaseDate()
        );
    }
}
