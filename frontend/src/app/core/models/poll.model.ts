export interface PollOption {
  id: number;
  optionText: string;
  voteCount: number;
}

export interface Poll {
  id: number;
  question: string;
  createdByName: string;
  status: 'OPEN' | 'CLOSED';
  closesAt: string | null;
  options: PollOption[];
  totalVotes: number;
  myVoteOptionId: number | null;
}

export interface PollCreateRequest {
  question: string;
  options: string[];
  closesAt?: string;
}
