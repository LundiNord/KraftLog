<?php

declare(strict_types=1);

namespace OCA\KraftLog\Controller;

use OCA\KraftLog\AppInfo\Application;
use OCP\AppFramework\Controller;
use OCP\AppFramework\Http\Attribute\NoAdminRequired;
use OCP\AppFramework\Http\Attribute\NoCSRFRequired;
use OCP\AppFramework\Http\TemplateResponse;
use OCP\IRequest;
use OCP\IURLGenerator;
use OCP\Util;

final class PageController extends Controller {
    public function __construct(
        string $appName,
        IRequest $request,
        private IURLGenerator $urlGenerator,
    ) {
        parent::__construct($appName, $request);
    }

    #[NoAdminRequired]
    #[NoCSRFRequired]
    public function index(): TemplateResponse {
        Util::addStyle(Application::APP_ID, 'kraftlog');
        Util::addScript(Application::APP_ID, 'kraftlog');

        $appUrl = rtrim(
            $this->urlGenerator->linkToRoute(Application::APP_ID . '.page.index'),
            '/',
        );

        return new TemplateResponse(Application::APP_ID, 'main', [
            'apiBase' => $appUrl . '/api',
        ]);
    }
}
