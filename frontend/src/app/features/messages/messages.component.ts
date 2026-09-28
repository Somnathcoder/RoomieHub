import { Component, inject, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from '../../core/services/message.service';
import { MemberService } from '../../core/services/member.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Message } from '../../core/models/message.model';
import { Member } from '../../core/models/member.model';
import { EmptyStateComponent } from '../../shared/components/empty-state.component';

@Component({
  selector: 'app-messages',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, EmptyStateComponent],
  template: `
    <div class="page" style="max-width: 1000px;">
      <div class="page-header">
        <div>
          <h1>Messages</h1>
          <div class="page-subtitle">Public room chat or private conversations.</div>
        </div>
      </div>

      <div class="grid" style="grid-template-columns: 220px 1fr; gap: 16px; align-items: start;">
        <div class="card" style="padding: 10px;">
          <button class="btn btn-sm w-full mb-1" [class.btn-primary]="activeChat() === 'public'" [class.btn-secondary]="activeChat() !== 'public'"
                  (click)="openPublic()">📢 Public room chat</button>
          <div class="text-muted" style="font-size:11px; text-transform:uppercase; padding: 8px 4px 4px;">Direct messages</div>
          @for (m of otherMembers(); track m.userId) {
            <button class="btn btn-sm w-full mb-1" [class.btn-primary]="activeChat() === m.userId" [class.btn-secondary]="activeChat() !== m.userId"
                    (click)="openPrivate(m)" style="justify-content:flex-start;">{{ m.fullName }}</button>
          }
        </div>

        <div class="card" style="min-height: 420px; display: flex; flex-direction: column;">
          <div style="flex:1; overflow-y:auto; max-height: 420px;">
            @if (messages().length === 0) {
              <app-empty-state icon="💬" title="No messages yet" subtitle="Say hello!" />
            } @else {
              @for (m of messages(); track m.id) {
                <div style="padding: 8px 4px; border-bottom: 1px solid var(--color-border);">
                  <div class="flex-between">
                    <strong style="font-size:13px;">{{ m.senderId === myUserId() ? 'You' : m.senderName }}</strong>
                    <span class="text-muted" style="font-size:11px;">{{ m.createdAt | date:'MMM d, h:mm a' }}</span>
                  </div>
                  <p style="margin: 2px 0 0;">{{ m.content }}</p>
                </div>
              }
            }
          </div>
          <form (ngSubmit)="send()" class="flex-gap mt-2">
            <input type="text" [(ngModel)]="draft" name="draft" placeholder="Type a message..." style="flex:1;" />
            <button type="submit" class="btn btn-primary" [disabled]="!draft.trim()">Send</button>
          </form>
        </div>
      </div>
    </div>
  `
})
export class MessagesComponent {
  private messageService = inject(MessageService);
  private memberService = inject(MemberService);
  auth = inject(AuthService);
  private toast = inject(ToastService);

  members = signal<Member[]>([]);
  messages = signal<Message[]>([]);
  activeChat = signal<'public' | number>('public');
  draft = '';

  myUserId() { return this.auth.currentUser()?.userId; }

  otherMembers() {
    return this.members().filter(m => m.userId !== this.myUserId());
  }

  constructor() {
    this.memberService.list().subscribe(list => this.members.set(list));
    this.openPublic();
  }

  openPublic() {
    this.activeChat.set('public');
    this.messageService.getPublic().subscribe(list => this.messages.set(list));
  }

  openPrivate(m: Member) {
    this.activeChat.set(m.userId);
    this.messageService.getConversation(m.userId).subscribe(list => this.messages.set(list));
  }

  send() {
    const content = this.draft.trim();
    if (!content) return;
    const chat = this.activeChat();
    const obs = chat === 'public' ? this.messageService.sendPublic(content) : this.messageService.sendPrivate(chat, content);
    obs.subscribe({
      next: () => { this.draft = ''; chat === 'public' ? this.openPublic() : this.openPrivate(this.members().find(m => m.userId === chat)!); },
      error: (err) => this.toast.error(err.error?.message ?? 'Could not send message.')
    });
  }
}
