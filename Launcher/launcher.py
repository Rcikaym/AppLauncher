import os
import subprocess
import time

def styled_cmd():
    os.system("start cmd /k title Launcher Status & color 0A")

def wait_step(message="Tekan Enter untuk melanjutkan ke tes berikutnya..."):
    input(f"\n>>> {message}")

def log(message, status="OK"):
    print(f"[{status}] {message}")

def open_camera():
    log("Membuka Kamera...")
    subprocess.Popen(["start", "microsoft.windows.camera:"], shell=True)

def open_task_manager():
    log("Membuka Task Manager...")
    os.system("start taskmgr")

def open_about():
    log("Membuka halaman Tentang Sistem...")
    os.system("start ms-settings:about")
def open_terminal_admin_serial():
    log("Membuka Windows Terminal (Admin) untuk cek Serial Number...")
    os.system(
        'powershell -Command "Start-Process wt -Verb RunAs -ArgumentList '
        '\'powershell -NoExit -Command Get-CimInstance Win32_BIOS ^| Select-Object SerialNumber\'"'
    )


def open_network():
    log("Membuka halaman Jaringan, WiFi, dan Bluetooth...")
    os.system("start ms-settings:network")

def open_activation():
    log("Membuka halaman Aktivasi Windows...")
    os.system("start ms-settings:activation")

def open_explorer():
    log("Membuka File Explorer...")
    os.system("start explorer")

def open_cmd_serial():
    log("Membuka CMD untuk pengecekan Serial Number...")
    os.system("start cmd /k wmic bios get serialnumber")

def open_disk_management():
    log("Membuka Manajemen Disk...")
    os.system("start diskmgmt.msc")

def safe_open(file):
    if os.path.exists(file):
        log(f"Menjalankan {file}...")
        subprocess.Popen(file)
    else:
        log(f"{file} tidak ditemukan", "LEWATI")


def main():
    styled_cmd()

    print(r"""
 ---------------------------------------
      Launcher by @hamsya_26 (IG)
 ---------------------------------------
""")

    log("Starting launch sequence...")
    time.sleep(0.5)


    # --- PARALLEL MULTI-WINDOW APPS ---
    open_camera()
    open_task_manager()
    open_explorer()
    open_cmd_serial()
    open_disk_management()
    open_terminal_admin_serial()

    safe_open(r"apps\keyboardtestutility.exe")
    safe_open(r"apps\aida64.exe")

    # --- SEQUENTIAL SETTINGS NAVIGATION ---
    open_about()
    wait_step("Sudah mengecek Info Sistem? Tekan Enter untuk tes Jaringan...")

    open_network()
    wait_step("Sudah mengecek WiFi/Bluetooth? Tekan Enter untuk halaman Aktivasi...")

    open_activation()
    wait_step("Sudah mengecek Aktivasi? Tekan Enter untuk melanjutkan...")

    log("Semua aplikasi berhasil dibuka!", "SELESAI")

    input("\nTekan Enter untuk keluar...")


if __name__ == "__main__":
    main()
