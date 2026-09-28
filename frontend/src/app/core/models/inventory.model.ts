export interface InventoryItem {
  id: number;
  itemName: string;
  quantity: number;
  addedByName: string;
  addedDate: string;
  photoUrl: string | null;
  condition: 'WORKING' | 'DAMAGED' | 'LOST' | 'DISPOSED';
  notes: string | null;
}

export interface InventoryItemCreateRequest {
  itemName: string;
  quantity?: number;
  condition?: string;
  notes?: string;
}
