#include <stdarg.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

typedef enum RequestChannelResult {
  Ok = 0,
  Full = 1,
  Closed = 2,
  NoSender = 3,
} RequestChannelResult;

typedef struct CByteArray CByteArray;

typedef struct RequestChannel RequestChannel;

typedef struct RpcTask RpcTask;

typedef struct RustChannel RustChannel;

typedef struct RustChannelBuilder RustChannelBuilder;

typedef struct RustMetadata RustMetadata;

typedef struct RustTlsConfigBuilder RustTlsConfigBuilder;

typedef struct CByteArray *(*EncodeMessage)(void*);

typedef void *(*DecodeMessage)(void*, const uint8_t *ptr, uintptr_t len);

typedef void (*RpcOnMessageReceived)(void *user_data, void *message);

typedef void (*RpcOnInitialMetadataReceived)(void *user_data, struct RustMetadata *metadata);

typedef void (*RpcOnDone)(void *user_data,
                          int32_t status_code,
                          const char *status_message,
                          struct RustMetadata *metadata,
                          struct RustMetadata *trailers);

typedef void (*FreeCByteArray)(void*);

void init(bool enable_trace_logs);

struct RpcTask *rpc_implementation(struct RustChannel *channel,
                                   const char *path,
                                   struct RustMetadata *metadata,
                                   struct RequestChannel *request_channel,
                                   void *user_data,
                                   EncodeMessage serialize_request,
                                   DecodeMessage deserialize_response,
                                   RpcOnMessageReceived on_message_received,
                                   RpcOnInitialMetadataReceived on_initial_metadata_received,
                                   RpcOnDone on_done);

struct RustChannelBuilder *channel_builder_create(const char *host,
                                                  bool enable_keepalive,
                                                  uint64_t keepalive_time_nanos,
                                                  uint64_t keepalive_timeout_nanos,
                                                  bool keepalive_without_calls);

struct RustTlsConfigBuilder *tls_config_create(void);

void tls_config_use_webpki_roots(struct RustTlsConfigBuilder *config);

bool tls_config_install_certificate(struct RustTlsConfigBuilder *config,
                                    const uint8_t *data,
                                    uintptr_t len);

bool tls_config_use_client_credentials(struct RustTlsConfigBuilder *config,
                                       const uint8_t *key_data,
                                       uintptr_t key_len,
                                       const uint8_t *cert_data,
                                       uintptr_t cert_len);

void channel_builder_use_tls_config(struct RustChannelBuilder *builder,
                                    struct RustTlsConfigBuilder *config);

struct RustChannel *channel_builder_build(struct RustChannelBuilder *builder);

void channel_free(struct RustChannel *channel);

struct RequestChannel *request_channel_create(void);

void request_channel_free(struct RequestChannel *ptr);

enum RequestChannelResult request_channel_send(struct RequestChannel *ptr, void *value);

void request_channel_signal_end(struct RequestChannel *ptr);

struct RustMetadata *metadata_create(const char *const *ascii_entries,
                                     const char *const *binary_keys,
                                     const uint8_t *const *binary_ptrs,
                                     const uintptr_t *binary_sizes);

void metadata_iterate(struct RustMetadata *metadata,
                      void *data,
                      void (*block_ascii)(void *data, const char *key, const char *value),
                      void (*block_binary)(void *data,
                                           const char *key,
                                           const uint8_t *ptr,
                                           uintptr_t size));

void metadata_free(struct RustMetadata *metadata);

void string_free(char *s);

void rpc_task_abort(struct RpcTask *task);

struct CByteArray *c_byte_array_create(void *data,
                                       const uint8_t *ptr,
                                       uintptr_t len,
                                       FreeCByteArray free);

void c_byte_array_free(struct CByteArray *ptr);
