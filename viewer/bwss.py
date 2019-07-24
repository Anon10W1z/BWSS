import configparser
import json
import os
from datetime import datetime
from tkinter import *
from tkinter import filedialog
from tkinter import messagebox


def get_stat_str(num, dem, places=3):
    s = str(round(float(num) / dem, places))
    decimals = len(s.split('.')[1])
    return s + '0' * (places - decimals)


def get_stats():
    global games_listbox
    global games
    global username_entry

    selected_games = [games[i] for i in games_listbox.curselection()]
    username = username_entry.get()

    total_games = 0
    total_minutes = 0.0

    total_kills = 0
    total_bed_breaks = 0
    total_final_kills = 0

    total_deaths = 0
    total_lost_bed = 0
    total_final_death = 0

    total_eliminated = 0
    for game in selected_games:
        players = game['players']
        for player in players:
            if player['username'] == username:
                total_games += 1
                total_minutes += float(player['endTime'] - player['startTime']) / 1000.0 / 60.0

                total_kills += player['kills']
                total_bed_breaks += player['bedBreaks']
                total_final_kills += player['finalKills']

                total_deaths += player['deaths']
                total_lost_bed += player['lostBed']
                total_final_death += player['finalDeath']

                total_eliminated += player['eliminated']

    if total_games == 0:
        messagebox.showerror('Error', 'No games contained player w/ username "' + username + '"')
        return

    global config
    config['DEFAULT']['username'] = username
    with open('bwss.ini', 'w') as f:
        config.write(f)

    total_points_scored = 0.5 * total_kills + 2.0 * total_bed_breaks + 2.0 * total_final_kills
    total_points_lost = 0.5 * total_deaths + 2.0 * total_lost_bed + 2.0 * total_final_death

    message = username + ' played in ' + str(total_games) + ' / ' + str(len(selected_games)) + ' games' + '\n'
    if total_games > 1:
        message += 'Total minutes played: ' + get_stat_str(total_minutes, 1, places=5) + '\n'
        message += 'Minutes per game: ' + get_stat_str(total_minutes, total_games) + '\n'
        message += '\n'
        message += 'Total points scored: ' + str(total_points_scored) + '\n'
        message += 'Total kills: ' + str(total_kills) + '\n'
        message += 'Total bed breaks: ' + str(total_bed_breaks) + '\n'
        message += 'Total final kills: ' + str(total_final_kills) + '\n'
        message += '\n'
        message += 'Points scored per game: ' + get_stat_str(total_points_scored, total_games) + '\n'
        message += 'Kills per game: ' + get_stat_str(total_kills, total_games) + '\n'
        message += 'Bed breaks per game: ' + get_stat_str(total_bed_breaks, total_games) + '\n'
        message += 'Final kills per game: ' + get_stat_str(total_final_kills, total_games) + '\n'
        message += '\n'
        message += 'Minutes per kill: ' + get_stat_str(total_minutes, total_kills) + '\n'
        message += 'Minutes per bed break: ' + get_stat_str(total_minutes, total_bed_breaks) + '\n'
        message += 'Minutes per final kill: ' + get_stat_str(total_minutes, total_final_kills) + '\n'
        message += 'Overall offensive efficiency: ' + get_stat_str(total_points_scored, total_minutes) + '\n'
        message += '\n'
        message += 'Total points lost: ' + str(total_points_lost) + '\n'
        message += 'Total deaths: ' + str(total_deaths) + '\n'
        message += 'Total lost beds: ' + str(total_lost_bed) + '\n'
        message += 'Total final deaths: ' + str(total_final_death) + '\n'
        message += '\n'
        message += 'Points lost per game: ' + get_stat_str(total_points_lost, total_games) + '\n'
        message += 'Deaths per game: ' + get_stat_str(total_deaths, total_games) + '\n'
        message += 'Lost beds per game: ' + get_stat_str(total_lost_bed, total_games) + '\n'
        message += 'Final deaths per game: ' + get_stat_str(total_final_death, total_games) + '\n'
        message += '\n'
        message += 'Minutes per death: ' + get_stat_str(total_minutes, total_deaths) + '\n'
        message += 'Minutes per lost bed: ' + get_stat_str(total_minutes, total_lost_bed) + '\n'
        message += 'Minutes per final death: ' + get_stat_str(total_minutes, total_final_death) + '\n'
        message += 'Overall defensive efficiency: ' + get_stat_str(total_points_lost, total_minutes) + '\n'
    else:
        message += 'Minutes played: ' + get_stat_str(total_minutes, 1, places=5) + '\n'
        message += '\n'
        message += 'Points scored: ' + str(total_points_scored) + '\n'
        message += 'Kills: ' + str(total_kills) + '\n'
        message += 'Bed breaks: ' + str(total_bed_breaks) + '\n'
        message += 'Final kills: ' + str(total_final_kills) + '\n'
        message += '\n'
        message += 'Minutes per kill: ' + get_stat_str(total_minutes, total_kills) + '\n'
        message += 'Minutes per bed break: ' + get_stat_str(total_minutes, total_bed_breaks) + '\n'
        message += 'Minutes per final kill: ' + get_stat_str(total_minutes, total_final_kills) + '\n'
        message += 'Offensive efficiency: ' + get_stat_str(total_points_scored, total_minutes) + '\n'
        message += '\n'
        message += 'Points lost: ' + str(total_points_lost) + '\n'
        message += 'Deaths: ' + str(total_deaths) + '\n'
        message += 'Lost beds: ' + str(total_lost_bed) + '\n'
        message += 'Final deaths: ' + str(total_final_death) + '\n'
        message += '\n'
        message += 'Minutes per death: ' + get_stat_str(total_minutes, total_deaths) + '\n'
        message += 'Minutes per lost bed: ' + get_stat_str(total_minutes, total_lost_bed) + '\n'
        message += 'Minutes per final death: ' + get_stat_str(total_minutes, total_final_death) + '\n'
        message += 'Defensive efficiency: ' + get_stat_str(total_points_lost, total_minutes) + '\n'

    message += '\n'
    message += 'Overall net efficiency: ' + get_stat_str(total_points_scored - total_points_lost, total_minutes) + '\n'
    message += 'Total eliminations: ' + str(total_eliminated) + '\n'
    message += 'Not-eliminated rate: ' + get_stat_str(total_games - total_eliminated, total_games)

    messagebox.showinfo('Stats', message)


def get_info_str(game):
    start_time = game['startTime']
    start_datetime = datetime.fromtimestamp(float(start_time) / 1000)
    return start_datetime.strftime('%Y-%m-%d %H:%M:%S') + ' - ' + game['mode'] + ' - ' + game['map']


def refresh_games(games_dir):
    if len(games_dir) == 0:
        return

    global games_listbox
    games_listbox.delete(0, END)

    global games
    games = []

    file_paths = [os.path.join(games_dir, f) for f in os.listdir(games_dir) if
                  os.path.isfile(os.path.join(games_dir, f))]
    for path in file_paths:
        if not path.endswith('json'):
            continue
        with open(path, 'r') as f:
            game = json.loads(f.read())
            games.append(game)

    games.sort(key=lambda game: game['startTime'], reverse=True)
    for game in games:
        games_listbox.insert(END, get_info_str(game))

    global config
    config['DEFAULT']['games_folder'] = games_dir
    with open('bwss.ini', 'w') as f:
        config.write(f)


def select_with_filter(predicate):
    global games_listbox
    global games
    for i in range(len(games)):
        games_listbox.selection_clear(i)
        if predicate(games[i]):
            games_listbox.selection_set(i)


def select_mode(mode):
    select_with_filter(lambda game: game['mode'] == mode)


def select_time(time):
    select_with_filter(lambda game: game['startTime'] > time)


def combine_predicates(p1, p2):
    return lambda game: p1(game) and p2(game)


def execute_custom():
    global custom_toplevel
    p = lambda game: True

    global bool_mode
    global mode_entry
    global last_bool_mode
    global last_mode_entry
    if bool_mode.get():
        p2 = lambda game: game['mode'] == mode_entry.get()
        p = combine_predicates(p, p2)
        last_bool_mode = bool_mode.get()
        last_mode_entry = mode_entry.get()

    global bool_map
    global map_entry
    global last_bool_map
    global last_map_entry
    if bool_map.get():
        p2 = lambda game: game['map'] == map_entry.get()
        p = combine_predicates(p, p2)
        last_bool_map = bool_map.get()
        last_map_entry = map_entry.get()

    global bool_hours
    global hours_entry
    global last_bool_hours
    global last_hours_entry
    if bool_hours.get():
        try:
            hours = float(hours_entry.get())
        except ValueError:
            hours = 24
        p2 = lambda game: game['startTime'] > 1000 * (datetime.timestamp(datetime.now()) - 60 * 60 * hours)
        p = combine_predicates(p, p2)
        last_bool_hours = bool_hours.get()
        last_hours_entry = hours_entry.get()

    select_with_filter(p)
    custom_toplevel.destroy()


def select_custom(compute_frame):
    global custom_toplevel
    custom_toplevel = Toplevel(compute_frame)
    custom_toplevel.title('Select Filters')
    i = 0

    global bool_mode
    global last_bool_mode
    bool_mode = BooleanVar(value=last_bool_mode)
    button = Checkbutton(custom_toplevel, text='Mode:', variable=bool_mode)
    button.grid(row=i, column=0)
    global mode_entry
    global last_mode_entry
    mode_entry = Entry(custom_toplevel, width=30)
    mode_entry.insert(END, last_mode_entry)
    mode_entry.grid(row=i, column=1)
    i += 1

    global bool_map
    global last_bool_map
    bool_map = BooleanVar(value=last_bool_map)
    button = Checkbutton(custom_toplevel, text='Map:', variable=bool_map)
    button.grid(row=i, column=0)
    global map_entry
    global last_map_entry
    map_entry = Entry(custom_toplevel, width=30)
    map_entry.insert(END, last_map_entry)
    map_entry.grid(row=i, column=1)
    i += 1

    global bool_hours
    global last_bool_hours
    bool_hours = BooleanVar(value=last_bool_hours)
    button = Checkbutton(custom_toplevel, text='Hours since:', variable=bool_hours)
    button.grid(row=i, column=0)
    global hours_entry
    global last_hours_entry
    hours_entry = Entry(custom_toplevel, width=30)
    hours_entry.insert(END, last_hours_entry)
    hours_entry.grid(row=i, column=1)
    i += 1

    exec_button = Button(custom_toplevel, text='Execute', command=execute_custom)
    exec_button.grid(row=i, column=0)


def popup_log(e):
    i = e.widget.curselection()[0]
    global games
    game = games[i]
    start_datetime = datetime.fromtimestamp(float(game['startTime']) / 1000)
    global config
    filename = os.path.join(config['DEFAULT']['games_folder'], game['logFileName'])
    log = ''
    try:
        with open(filename) as f:
            log = ''.join(f.readlines())
    except IOError:
        log = '\n'.join(game['logMessages'])
    popup = Toplevel(width=50, height=50)
    popup.title(get_info_str(game))
    scrollbar = Scrollbar(popup)
    scrollbar.pack(side=RIGHT, fill=Y)
    log_text = Text(popup, state='disabled', yscrollcommand=scrollbar.set)
    log_text.configure(state='normal')
    log_text.insert(END, log)
    log_text.configure(state='disabled')
    log_text.pack(expand=True, fill='both')
    scrollbar.config(command=log_text.yview)


def main():
    global last_bool_mode
    last_bool_mode = False
    global last_mode_entry
    last_mode_entry = ''
    global last_bool_map
    last_bool_map = False
    global last_map_entry
    last_map_entry = ''
    global last_bool_hours
    last_bool_hours = False
    global last_hours_entry
    last_hours_entry = ''

    global config
    config = configparser.ConfigParser()
    dataset = config.read('bwss.ini')
    if len(dataset) == 0:
        config['DEFAULT']['games_folder'] = ''
        config['DEFAULT']['username'] = ''
        with open('bwss.ini', 'w') as f:
            config.write(f)
        dataset = config.read('bwss.ini')

    global games
    games = []

    root = Tk()
    root.title('bwss')
    frame = Frame(root)
    frame.pack()

    games_frame = Frame(frame)
    games_frame.pack(side=LEFT)

    scrollbar = Scrollbar(games_frame)
    scrollbar.pack(side=RIGHT, fill=Y)

    global games_listbox
    games_listbox = Listbox(games_frame, selectmode=EXTENDED, width=40, height=30)
    games_listbox.bind('<Double-Button-1>', popup_log)
    refresh_games(config['DEFAULT']['games_folder'])
    games_listbox.pack(side=LEFT)
    games_listbox.config(yscrollcommand=scrollbar.set)
    scrollbar.config(command=games_listbox.yview)

    choose_button = Button(frame, text='Choose Directory', command=lambda: refresh_games(
        filedialog.askdirectory(initialdir=config['DEFAULT']['games_folder'])))
    choose_button.pack(side=TOP)

    compute_frame = Frame(frame)
    compute_frame.pack(side=BOTTOM)
    filter_none = Button(compute_frame, text='Select All', command=lambda: select_with_filter(lambda game: True))
    filter_none.pack()
    filter_last_day = Button(compute_frame, text='Select Last 24h',
                             command=lambda: select_time(1000 * (datetime.timestamp(datetime.now()) - 60 * 60 * 24)))
    filter_last_day.pack()
    filter_solo = Button(compute_frame, text='Select Solos', command=lambda: select_mode('Solo'))
    filter_solo.pack()
    filter_fours = Button(compute_frame, text='Select Fours', command=lambda: select_mode('4v4v4v4'))
    filter_fours.pack()
    filter_custom = Button(compute_frame, text='Select...', command=lambda: select_custom(compute_frame))
    filter_custom.pack()
    username_message = Message(compute_frame, text='Username:', width=800)
    username_message.pack()
    global username_entry
    username_entry = Entry(compute_frame)
    username_entry.insert(END, config['DEFAULT']['username'])
    username_entry.pack()
    stats_button = Button(compute_frame, text='Get Stats', command=get_stats)
    stats_button.pack()

    root.mainloop()


if __name__ == '__main__':
    main()
