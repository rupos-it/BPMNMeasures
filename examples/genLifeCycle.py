#!/usr/bin/python

from generator import Sequence, Choice, Entry
from generator import generate
from generator import defaultActivityHook

def task2_hook(startTime, attrs):
    t0 = defaultActivityHook(startTime, attrs)
    t0 += 15*60
    return t0

process = Sequence([
	Entry("secondotask"),
	Entry("task1", task2_hook)
	])
generate(process, 100)
