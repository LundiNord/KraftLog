<?php

declare(strict_types=1);

namespace OCA\KraftLog\Listener;

use OCA\KraftLog\Service\KraftLogService;
use OCP\EventDispatcher\Event;
use OCP\EventDispatcher\IEventListener;
use OCP\User\Events\BeforeUserDeletedEvent;
use OCP\User\Events\BeforeUserIdUnassignedEvent;

/**
 * Remove private workout data before a Nextcloud identity disappears, so a
 * later account reusing the same UID cannot inherit it.
 *
 * @template-implements IEventListener<BeforeUserDeletedEvent|BeforeUserIdUnassignedEvent>
 */
final class UserLifecycleListener implements IEventListener {
    public function __construct(
        private KraftLogService $service,
    ) {
    }

    public function handle(Event $event): void {
        if ($event instanceof BeforeUserDeletedEvent) {
            $this->service->deleteUserData($event->getUser()->getUID());
        } elseif ($event instanceof BeforeUserIdUnassignedEvent) {
            $this->service->deleteUserData($event->getUserId());
        }
    }
}
