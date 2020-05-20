#!/usr/bin/env bash
# gunicorn3 --log-level debug --bind 127.0.0.1:5000 app:app
env GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/gcp-auth.json gunicorn3 --bind 127.0.0.1:5000 app:app
