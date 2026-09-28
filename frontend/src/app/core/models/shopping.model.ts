export interface ShoppingItem {
  id: number;
  itemName: string;
  quantity: string | null;
  estimatedAmount: number | null;
  addedByName: string;
  status: 'PENDING' | 'PURCHASED';
  itemPhotoUrl: string | null;
  billPhotoUrl: string | null;
  purchaseDate: string | null;
}

export interface ShoppingItemCreateRequest {
  itemName: string;
  quantity?: string;
  estimatedAmount?: number;
}
