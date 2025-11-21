FROM fluent/fluentd:v1.16-debian-1

USER root

# Install dependencies and plugins
# - Add curl (used by HEALTHCHECK)
# - Install build tools for native extensions, install required gems in one step
# - Use --no-document to reduce image size
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates \
    build-essential \
    ruby-dev && \
    gem install fluentd -v 1.16.2 && \
    gem install elasticsearch -v 8.10.0 && \
    gem install fluent-plugin-elasticsearch -v 5.4.3 && \
    gem install fluent-plugin-rewrite-tag-filter && \
    gem sources --clear-all && \
    apt-get remove -y build-essential ruby-dev && \
    chown -R fluent:fluent /fluentd/log

USER fluent

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:24220/api/plugins.json || exit 1