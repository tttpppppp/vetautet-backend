import re
import os

file_path = r'd:\Java\train_spring\VeTau-v1\environment\mysql\init\vetautet-db.sql'

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Generate seats
    seats = []
    # Toa 1
    for i in range(1, 51):
        seats.append(f"(1, 'A{i}', 'SOFT_SEAT')")
    # Toa 2
    for i in range(1, 51):
        seats.append(f"(2, 'G{i}', 'SLEEPER')")
    # Toa 3
    for i in range(1, 51):
        seats.append(f"(3, 'C{i}', 'HARD_SEAT')")

    seats_sql = "INSERT INTO `seats` (`carriage_id`, `seat_number`, `seat_type`) VALUES \n" + ",\n".join(seats) + ";"

    # Replace seats section
    new_content = re.sub(
        r'(?<=-- 4\. Ghế ngồi mẫu cho Toa 1 \(5 hàng x 4 cột = 20 ghế\)\n)(.*?)(?=-- 5\. Chuyến đi mẫu \(Trip\))',
        f"-- Sinh 50 ghế cho mỗi toa\n{seats_sql}\n\n",
        content,
        flags=re.DOTALL
    )

    # Generate tickets
    tickets = []
    # Mặc định SEAT_ID insert từ 1 đến 150
    # Chuyến đi 1 (train 1 => toa 1 & 2): 100 vé
    for i in range(1, 51):
        tickets.append(f"(1, {i}, 500000, 'AVAILABLE')")
    for i in range(51, 101):
        tickets.append(f"(1, {i}, 1200000, 'AVAILABLE')")

    # Chuyến đi 2 (train 2 => toa 3): 50 vé
    for i in range(101, 151):
        tickets.append(f"(2, {i}, 300000, 'AVAILABLE')")

    tickets_sql = "INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`) VALUES \n" + ",\n".join(tickets) + ";"

    new_content = re.sub(
        r'(?<=-- 7\. Trạng thái Vé thực tế \(Tickets\) - Kết nối Trip và Seat\n)(.*?)(?=-- 8\. Chi tiết đợt Flash Sale)',
        f"-- Sinh vé cho 50 ghế mỗi toa\n{tickets_sql}\n\n",
        new_content,
        flags=re.DOTALL
    )

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print("Updated vetautet-db.sql successfully")

except Exception as e:
    print(f"Error: {e}")
