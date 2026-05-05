#!/bin/bash
set -euo pipefail

if [ "${CONFIGURATION}" = "Release" ]; then
    CRASHLYTICS_RUN="${BUILD_DIR%Build/*}SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
    if [ -x "${CRASHLYTICS_RUN}" ]; then
        "${CRASHLYTICS_RUN}" "${DWARF_DSYM_FOLDER_PATH}"
    else
        echo "Crashlytics run script not found at ${CRASHLYTICS_RUN}; skipping dSYM upload."
    fi
fi
