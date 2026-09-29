package com.roommate.management.service;

import com.roommate.management.dto.request.InventoryItemCreateRequest;
import com.roommate.management.dto.request.InventoryItemUpdateRequest;
import com.roommate.management.dto.response.InventoryItemResponse;
import com.roommate.management.entity.InventoryItem;
import com.roommate.management.entity.RoomMember;
import com.roommate.management.entity.enums.*;
import com.roommate.management.exception.ResourceNotFoundException;
import com.roommate.management.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final RoomAccessService roomAccessService;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;

    @Transactional
    public InventoryItemResponse create(Long userId, InventoryItemCreateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_INVENTORY);

        InventoryItem item = InventoryItem.builder()
                .room(caller.getRoom())
                .itemName(request.itemName())
                .quantity(request.quantity() == null ? 1 : request.quantity())
                .addedDate(LocalDate.now())
                .addedBy(caller)
                .condition(request.condition() == null ? InventoryCondition.WORKING : request.condition())
                .notes(request.notes())
                .build();
        item = inventoryItemRepository.save(item);
        activityLogService.log(caller.getRoom(), caller.getUser(), ActivityModule.INVENTORY, "Inventory item added", item.getId(),
                caller.getUser().getFullName() + " added \"" + item.getItemName() + "\" to inventory");
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> list(Long userId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        return inventoryItemRepository.findByRoomIdOrderByCreatedAtDesc(caller.getRoom().getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public InventoryItemResponse update(Long userId, Long itemId, InventoryItemUpdateRequest request) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_INVENTORY);
        InventoryItem item = inventoryItemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        roomAccessService.requireSameRoom(caller, item.getRoom().getId());

        if (request.itemName() != null) item.setItemName(request.itemName());
        if (request.quantity() != null) item.setQuantity(request.quantity());
        if (request.condition() != null) item.setCondition(request.condition());
        if (request.notes() != null) item.setNotes(request.notes());

        return toResponse(inventoryItemRepository.save(item));
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_INVENTORY);
        InventoryItem item = inventoryItemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        roomAccessService.requireSameRoom(caller, item.getRoom().getId());
        inventoryItemRepository.delete(item);
    }

    @Transactional
    public String uploadPhoto(Long userId, Long itemId, MultipartFile file) {
        RoomMember caller = roomAccessService.getActiveMembership(userId);
        roomAccessService.requirePermission(caller, PermissionCode.MANAGE_INVENTORY);
        InventoryItem item = inventoryItemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        roomAccessService.requireSameRoom(caller, item.getRoom().getId());
        String path = fileStorageService.store(file, "inventory", List.of("image/jpeg", "image/png", "image/jpg"));
        item.setPhotoUrl(fileStorageService.toPublicUrl(path));
        inventoryItemRepository.save(item);
        return item.getPhotoUrl();
    }

    private InventoryItemResponse toResponse(InventoryItem i) {
        return new InventoryItemResponse(i.getId(), i.getItemName(), i.getQuantity(), i.getAddedBy().getUser().getFullName(),
                i.getAddedDate(), i.getPhotoUrl(), i.getCondition().name(), i.getNotes());
    }
}
