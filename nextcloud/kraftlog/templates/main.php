<?php

declare(strict_types=1);
?>
<div
    id="kraftlog-app"
    class="kraftlog-app"
    data-api-base="<?php p($_['apiBase']); ?>"
>
    <div class="kraftlog-loading" role="status">
        <span class="kraftlog-spinner" aria-hidden="true"></span>
        <span><?php p($l->t('Loading KraftLog…')); ?></span>
    </div>
</div>
