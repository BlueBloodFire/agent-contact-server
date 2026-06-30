"""
一键部署脚本：本地 Maven 构建 -> SFTP 上传 JAR -> 服务器 Docker build -> 重启后端

用法：
  py -3 deploy.py           # 完整流程（构建 + 上传 + 部署）
  py -3 deploy.py --skip-build  # 跳过 Maven 构建（使用已有 JAR）
"""

import subprocess, sys, os, socket, paramiko, time, re, select

# ── 配置 ──────────────────────────────────────────────────────────────────────
SERVER_HOST = '124.220.1.39'
SERVER_USER = 'ubuntu'
SERVER_PASS = 'Wangjin@test'

LOCAL_JAR   = os.path.join(os.path.dirname(__file__),
              'ai-agent-contact-app', 'target', 'agent-scaffold-app.jar')
REMOTE_JAR  = '/opt/deploy/ai-agent-contact/ai-agent-contact-app/target/agent-scaffold-app.jar'
REMOTE_SRC  = '/opt/deploy/ai-agent-contact'
DOCKER_IMG  = 'system/ai-agent-contact:1.0'
COMPOSE_FILE = '/opt/deploy/docker-compose-app.yml'

DOCKERFILE = """\
FROM openjdk:17-jdk-slim
ENV PARAMS="" TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY ai-agent-contact-app/target/agent-scaffold-app.jar /agent-scaffold-app.jar
EXPOSE 8092
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS /agent-scaffold-app.jar $PARAMS"]
"""

# ── 帮助函数 ──────────────────────────────────────────────────────────────────
def step(msg):
    print(f'\n{"="*60}\n{msg}\n{"="*60}')

def run_remote(t, cmd, wait=10):
    chan = t.open_session()
    chan.exec_command(cmd)
    time.sleep(wait)
    out = b''
    err = b''
    while chan.recv_ready(): out += chan.recv(65536)
    while chan.recv_stderr_ready(): err += chan.recv_stderr(65536)
    chan.close()
    return (out + err).decode('utf-8', errors='replace').strip()

def stream_remote(t, cmd, timeout=120):
    chan = t.open_session()
    chan.exec_command(cmd)
    start = time.time()
    while True:
        r, _, _ = select.select([chan], [], [], 3)
        if r:
            while chan.recv_ready():
                sys.stdout.write(chan.recv(8192).decode('utf-8', errors='replace'))
                sys.stdout.flush()
        if chan.exit_status_ready():
            while chan.recv_ready():
                sys.stdout.write(chan.recv(8192).decode('utf-8', errors='replace'))
                sys.stdout.flush()
            break
        if time.time() - start > timeout:
            print(f'\n[超时 {timeout}s]')
            break
    return chan.recv_exit_status()

# ── 1. 本地 Maven 构建 ────────────────────────────────────────────────────────
skip_build = '--skip-build' in sys.argv

if not skip_build:
    step('1/4  本地 Maven 构建')
    project_root = os.path.dirname(__file__)
    result = subprocess.run(
        ['mvn', 'clean', 'package', '-DskipTests'],
        cwd=project_root,
        text=True
    )
    if result.returncode != 0:
        print('Maven 构建失败，终止部署。')
        sys.exit(1)
    print('Maven 构建成功。')
else:
    step('1/4  跳过 Maven 构建')

if not os.path.exists(LOCAL_JAR):
    print(f'JAR 不存在: {LOCAL_JAR}')
    sys.exit(1)

jar_size_mb = os.path.getsize(LOCAL_JAR) // 1024 // 1024
print(f'JAR: {LOCAL_JAR}  ({jar_size_mb} MB)')

# ── 连接服务器 ────────────────────────────────────────────────────────────────
print('\n连接服务器...')
sock = socket.socket()
sock.settimeout(30)
sock.connect((SERVER_HOST, 22))
t = paramiko.Transport(sock)
t.start_client()
t.auth_password(SERVER_USER, SERVER_PASS)
print(f'已连接 {SERVER_HOST}')

# ── 2. 上传 JAR ───────────────────────────────────────────────────────────────
step('2/4  上传 JAR 到服务器')

class UploadProgress:
    def __init__(self):
        self.last_pct = -1
    def __call__(self, sent, total):
        pct = int(sent * 100 / total)
        if pct != self.last_pct and pct % 10 == 0:
            print(f'  {pct}%  ({sent // 1024 // 1024}MB / {total // 1024 // 1024}MB)')
            self.last_pct = pct

sftp = paramiko.SFTPClient.from_transport(t)
sftp.put(LOCAL_JAR, REMOTE_JAR, callback=UploadProgress())
sftp.close()
print('上传完成。')

# ── 3. Docker build ───────────────────────────────────────────────────────────
step('3/4  服务器 Docker build')

# 写入 Dockerfile
run_remote(t, f"cat > /tmp/deploy.Dockerfile << 'EOF'\n{DOCKERFILE}\nEOF", wait=3)

code = stream_remote(
    t,
    f'cd {REMOTE_SRC} && sudo docker build -f /tmp/deploy.Dockerfile -t {DOCKER_IMG} . 2>&1',
    timeout=120
)
if code != 0:
    print(f'\nDocker build 失败 (exit {code})，终止部署。')
    t.close(); sock.close(); sys.exit(1)
print('\nDocker build 成功。')

# ── 4. 重新部署容器 ────────────────────────────────────────────────────────────
step('4/4  重启后端容器')

out = run_remote(t, f'cd /opt/deploy && sudo docker-compose -f {COMPOSE_FILE} up -d --force-recreate backend 2>&1', wait=15)
print(out)

print('\n等待应用启动 (60s)...')
time.sleep(60)

# 检查启动结果
log = run_remote(t, 'sudo docker logs ai-agent-contact 2>&1 | grep -E "Started Application|Application run failed" | tail -3', wait=5)
print(log)

if 'Started Application' in log:
    print('\n[OK] Backend started successfully.')
    resp = run_remote(t, """curl -s -X POST http://localhost:8093/api/v1/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin"}'""", wait=8)
    if '"0000"' in resp:
        print('[OK] Login API works.')
    else:
        print(f'Login response: {resp}')
else:
    print('\n[WARN] No startup success log detected, please verify manually.')
    print(run_remote(t, 'sudo docker logs ai-agent-contact --tail 20 2>&1', wait=5))

t.close()
sock.close()
print('\n部署完成。')
