#!/usr/bin/env bash
# gunicorn3 --log-level debug --bind 127.0.0.1:5000 app:app
env GOOGLE_APPLICATION_CREDENTIALS=/home/holdmusic-proxy/HoldMusic/director/gcp-auth.json gunicorn --bind 127.0.0.1:5000 app:app
